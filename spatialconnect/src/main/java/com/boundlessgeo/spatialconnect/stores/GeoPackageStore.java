/**
 * Copyright 2015-2017 Boundless, http://boundlessgeo.com
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.stores;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.db.GeoPackage;
import com.boundlessgeo.spatialconnect.db.GeoPackageContents;
import com.boundlessgeo.spatialconnect.db.SCGpkgFeatureSource;
import com.boundlessgeo.spatialconnect.db.SCSqliteHelper;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCPolygon;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.style.SCStyle;
import com.boundlessgeo.spatialconnect.tiles.GpkgTileProvider;
import com.boundlessgeo.spatialconnect.tiles.SCGpkgTileSource;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

import org.sqlite.database.SQLException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Provides capabilities for interacting with a single GeoPackage.
 */
public class GeoPackageStore extends SCDataStore implements ISCSpatialStore, SCDataStoreLifeCycle, SCRasterStore, ISyncableStore {

    private static final String LOG_TAG = GeoPackageStore.class.getSimpleName();
    public static final String TYPE = "gpkg";
    private static final String VERSION = "1";
    protected GeoPackage gpkg;
    protected SCStoreConfig scStoreConfig;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    /**
     * Constructor for GeoPackageStore that initializes the data store adapter
     * based on the scStoreConfig.
     *
     * @param context       instance of the current activity's context
     * @param scStoreConfig instance of the configuration needed to configure the store
     */
    public GeoPackageStore(Context context, SCStoreConfig scStoreConfig) {
        this(context, scStoreConfig, null);
    }

    public GeoPackageStore(Context context, SCStoreConfig scStoreConfig, SCStyle style) {
        super(context, scStoreConfig);
        this.scStoreConfig = scStoreConfig;
        this.setName(scStoreConfig.getName());
        this.setType(TYPE);
        this.setVersion(scStoreConfig.getVersion());
        this.getKey();
        this.style = style;
    }

    public List<String> layers() {
        List<String> allLayers = new ArrayList<>();
        allLayers.addAll(this.vectorLayers());
        allLayers.addAll(this.rasterLayers());
        return allLayers;
    }

    public List<String> vectorLayers() {
        Map<String, SCGpkgFeatureSource> fs = getFeatureSources();
        return new ArrayList<>(fs.keySet());
    }

    public List<String> rasterLayers() {
        Map<String, SCGpkgTileSource> fs = getTileSources();
        return new ArrayList<>(fs.keySet());
    }

    public Set<GeoPackageContents> getGeoPackageContents() {
        return gpkg.getGeoPackageContents();
    }

    public Map<String, SCGpkgFeatureSource> getFeatureSources() {
        if (gpkg != null) {
            return gpkg.getFeatureSources();
        } else {
            return new HashMap<>();
        }
    }

    public Map<String, SCGpkgTileSource> getTileSources() {
        if (gpkg !=null) {
            return gpkg.getTileSources();
        } else {
            return new HashMap<>();
        }
    }

    public void addLayer(String layer, Map<String,String>  fields) {
        gpkg.addFeatureSource(layer, fields);
    }

    public void deleteLayer(String layer) {
        BriteDatabase.Transaction tx = gpkg.newTransaction();
        // first remove from gpkg_geometry_columns
        gpkg.removeFromGpkgGeometryColumns(layer);
        // then remove it from geopackage contents
        gpkg.removeFromGpkgContents(layer);
        // lastly, remove the table itself so there are no FK constraint violations
        gpkg.query("DROP TABLE " + layer);
        tx.markSuccessful();
        tx.end();
        gpkg.refreshFeatureSources();
    }

    public String getFilePath() {
        return getContext().getDatabasePath(scStoreConfig.getUniqueID()).getPath();
    }

    @Override
    public DataStorePermissionEnum getAuthorization() {
        return DataStorePermissionEnum.READ_WRITE;
    }

    @Override
    public Observable<SCSpatialFeature> query(final SCQueryFilter queryFilter) {
        final Map<String, SCGpkgFeatureSource> layers = gpkg.getFeatureSources();

        if (layers.size() > 0) {  // ensure only layers with feature sources are queried
            // if there are no layer names supplied in the query filter, then search on all feature sources
            final List<String> featureTableNames = queryFilter.getLayerIds().size() > 0 ?
                    queryFilter.getLayerIds() :
                    new ArrayList<>(layers.keySet());
            // TODO: decide on what to do if queryLimit division is 0
            final int queryLimit = queryFilter.getLimit() / featureTableNames.size();
            Log.d(LOG_TAG, "querying on feature tables: " + featureTableNames.toString());
            return Observable.from(featureTableNames)
                    .filter(new Func1<String, Boolean>() {
                        @Override
                        public Boolean call(String featureTableName) {
                            return layers.containsKey(featureTableName);
                        }
                    })
                    .flatMap(new Func1<String, Observable<SCSpatialFeature>>() {
                        @Override
                        public Observable<SCSpatialFeature> call(final String layerName) {
                            final SCGpkgFeatureSource featureSource = gpkg.getFeatureSourceByName(layerName);
                            return gpkg.createQuery(
                                    layerName,
                                    String.format(
                                            "SELECT %s FROM %s WHERE %s IN (%s) LIMIT %d",
                                            getSelectColumnsString(featureSource),
                                            layerName,
                                            featureSource.getPrimaryKeyName(),
                                            createRtreeSubQuery(featureSource, queryFilter.getPredicate().getBoundingBox()),
                                            queryLimit
                                    )
                            ).flatMap(getFeatureMapper(featureSource)).onBackpressureBuffer(queryFilter.getLimit());
                        }
                    });
        }
        else {
            // can't query on geopackages with no features
            return Observable.empty();
        }
    }

    @Override
    public Observable<SCSpatialFeature> queryById(final SCKeyTuple keyTuple) {
        final String tableName = keyTuple.getLayerId();
        final SCGpkgFeatureSource featureSource = gpkg.getFeatureSourceByName(tableName);
        if (featureSource == null) {
            return Observable.error(
                    new SCDataStoreException(
                            SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                            String.format("%s was not a valid feature table name.", tableName)
                    )
            );
        }
        else {
            String query;
            Map<String, String> geometrycolumns = featureSource.getGeometryColumns();
            Map<String, String> nonGeometrycolumns = featureSource.getColumns();
            List<String> geomTables = featureSource.getGeometryTables();

            if (geometrycolumns.size() > 0) {
                StringBuilder sql = new StringBuilder();
                sql.append("SELECT ");
                boolean firstIteration = true;
                if (nonGeometrycolumns.size() > 0) {
                    for (Map.Entry<String, String> nonGeom : nonGeometrycolumns.entrySet()) {
                        if (firstIteration) {
                            sql.append(nonGeom.getKey());
                            firstIteration = false;
                        } else {
                            sql.append(",");
                            sql.append(nonGeom.getKey());
                        }
                    }

                }

                //add id
                sql.append(",");
                sql.append(featureSource.getPrimaryKeyName());

                //add default form submission geometry
                sql.append(",");
                sql.append(featureSource.getGeomColumnName());

                //add other gemetry columsn
                if (geometrycolumns.size() > 0) {

                    for (Map.Entry<String, String> geom : geometrycolumns.entrySet()) {
                        if (firstIteration) {
                            sql.append("ST_AsBinary(").append(geom.getKey()).append(") AS ").append(geom.getKey());
                            firstIteration = false;
                        } else {
                            sql.append(",");
                            sql.append("ST_AsBinary(").append(geom.getKey()).append(") AS ").append(geom.getKey());
                        }
                    }

                }
                sql.append(" FROM ");
                sql.append(tableName);

                for (String gt : geomTables) {
                //for (Map.Entry<String, String> gt : geomTables.entrySet()) {
                    sql.append(" LEFT JOIN ");
                    sql.append(gt);
                    sql.append(" ON ");
                    sql.append(String.format("%s.id", tableName));
                    sql.append(" = ");
                    sql.append(String.format("%s.%s_id", gt, tableName));
                }
                sql.append(String.format(" WHERE %s.%s = %s LIMIT 1",
                        tableName,
                        featureSource.getPrimaryKeyName(),
                        keyTuple.getFeatureId())) ;
                query = sql.toString();
            } else {
                query = String.format(
                        "SELECT %s FROM %s WHERE %s = %s LIMIT 1",
                        getSelectColumnsString(featureSource),
                        tableName,
                        featureSource.getPrimaryKeyName(),
                        keyTuple.getFeatureId()
                );
            }
            Log.e(LOG_TAG, "query: " + query);
            return gpkg.createQuery(
                    tableName,
                    query
            ).flatMap(getFeatureMapper(featureSource));
        }
    }

    @Override
    public Observable<SCSpatialFeature> create(final SCSpatialFeature scSpatialFeature) {
        final String tableName = scSpatialFeature.getKey().getLayerId();
        final SCGpkgFeatureSource featureSource = gpkg.getFeatureSourceByName(tableName);
        if (featureSource == null) {
            Log.e("Sync", "featureSource null");
            return Observable.error(
                    new SCDataStoreException(
                            SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                            String.format("%s was not a valid feature table name.", tableName)
                    )
            );
        }
        else {
            return Observable.create(new Observable.OnSubscribe<SCSpatialFeature>() {
                @Override
                public void call(Subscriber<? super SCSpatialFeature> subscriber) {
                    try {


                        Set<String> columns = featureSource.getColumns().keySet();
                        Map<String, Object> props = new HashMap<>();
                        for (String col : columns) {
                            if (!col.equals(featureSource.getGeomColumnName()) && !col.equals(featureSource.getPrimaryKeyName())) {
                                props.put(col, null);
                            }
                        }
                        String columnNames = featureSource.getColumnNamesForInsert(scSpatialFeature);
                        String columnValues = featureSource.getColumnValuesForInsert(scSpatialFeature);

                        String[] properties = columnNames.split(",");
                        //ensure feature columns + geom column equal the column names otherwise something is wrong
                        if ((props.size() + 1 )!= properties.length) {
                            subscriber.onError(new Throwable("Invalid column names or values"));
                        }


                        gpkg.executeAndTrigger(tableName,
                                String.format("INSERT OR REPLACE INTO %s (%s) VALUES (%s)",
                                        tableName,
                                        columnNames,
                                        columnValues
                                )
                        );
                        // get the id of the last inserted row: https://www.sqlite.org/lang_corefunc.html#last_insert_rowid
                        Cursor cursor = gpkg.query("SELECT last_insert_rowid()");
                        if (cursor != null) {
                            cursor.moveToFirst();  // force query to execute
                            final Integer pk = cursor.getInt(0);
                            scSpatialFeature.setId(String.valueOf(pk));

                            //Check for geometry properties
                            for (Map.Entry<String, Object> entry : scSpatialFeature.getProperties().entrySet()) {
                                if (entry.getValue() instanceof SCGeometry && ((SCGeometry)entry.getValue()).getGeometry() != null) {
                                    String geometryFieldTableName = String.format("%s_%s", tableName, entry.getKey());
                                    StringBuilder colNames = new StringBuilder();
                                    colNames.append(tableName);
                                    colNames.append("_id, ");
                                    colNames.append(entry.getKey());


                                    StringBuilder sb = new StringBuilder();
                                    sb.append(scSpatialFeature.getId());
                                    sb.append(", ");
                                    sb.append("ST_GeomFromText('")
                                            .append(((SCGeometry)entry.getValue()).getGeometry().toString())
                                            .append("')");

                                    String colValue = sb.toString();
                                    Log.e(LOG_TAG, "INSERT query: " + String.format("INSERT OR REPLACE INTO %s (%s) VALUES (%s)",
                                            geometryFieldTableName,
                                            colNames.toString(),
                                            colValue
                                    ));
                                    gpkg.executeAndTrigger(geometryFieldTableName,
                                            String.format("INSERT OR REPLACE INTO %s (%s) VALUES (%s)",
                                                    geometryFieldTableName,
                                                    colNames.toString(),
                                                    colValue
                                            )
                                    );

                                }
                            }

                            subscriber.onNext(scSpatialFeature);
                            subscriber.onCompleted();
                        }

                        storeEdited.onNext(scSpatialFeature);
                    }
                    catch (SQLException ex) {
                        subscriber.onError(new Throwable("Could not create the the feature.", ex));
                    }
                }
            });
        }
    }

    @Override
    public Observable<SCSpatialFeature> update(final SCSpatialFeature scSpatialFeature) {
        final String tableName = scSpatialFeature.getKey().getLayerId();
        final SCGpkgFeatureSource featureSource = gpkg.getFeatureSourceByName(tableName);
        if (featureSource == null) {
            return Observable.error(
                    new SCDataStoreException(
                            SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                            String.format("%s was not a valid feature table name.", tableName)
                    )
            );
        }
        else {
            return Observable.create(new Observable.OnSubscribe<SCSpatialFeature>() {
                @Override
                public void call(Subscriber<? super SCSpatialFeature> subscriber) {
                    try {
                        gpkg.executeAndTrigger(tableName,
                                String.format("UPDATE %s SET %s WHERE %s = %s",
                                        tableName,
                                        featureSource.getUpdateSetClause(scSpatialFeature),
                                        featureSource.getPrimaryKeyName(),
                                        scSpatialFeature.getId()
                                )
                        );
                        subscriber.onNext(scSpatialFeature);
                        subscriber.onCompleted();
                    }
                    catch (SQLException ex) {
                        subscriber.onError(new Throwable("Could not update the feature.", ex));
                    }
                }
            });
        }
    }

    @Override
    public Observable<Void> delete(final SCKeyTuple keyTuple) {
        final String tableName = keyTuple.getLayerId();
        final SCGpkgFeatureSource featureSource = gpkg.getFeatureSourceByName(tableName);
        if (featureSource == null) {
            return Observable.error(
                    new SCDataStoreException(
                            SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                            String.format("%s was not a valid feature table name.", tableName)
                    )
            );
        }
        else {
            return Observable.create(new Observable.OnSubscribe<Void>() {
                @Override
                public void call(Subscriber<? super Void> subscriber) {
                    try {
                        gpkg.executeAndTrigger(tableName,
                                String.format("DELETE FROM %s WHERE %s = %s",
                                        tableName,
                                        featureSource.getPrimaryKeyName(),
                                        keyTuple.getFeatureId()
                                )
                        );
                        subscriber.onCompleted();
                    }
                    catch (SQLException ex) {
                        subscriber.onError(new Throwable("Could not delete the feature.", ex));
                    }
                }
            });
        }
    }

    @Override
    public Observable<SCStoreStatusEvent> start() {
        final String storeId = this.getStoreId();
        final GeoPackageStore storeInstance = this;

        Log.d(LOG_TAG, "Starting store " + this.getName());
        storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_STARTED);


        return Observable.create(new Observable.OnSubscribe<SCStoreStatusEvent>() {

            @Override
            public void call(final Subscriber<? super SCStoreStatusEvent> subscriber) {

            // The db name on disk is its store ID to guarantee uniqueness on disk
            if (getContext().getDatabasePath(scStoreConfig.getUniqueID()).exists()) {
                Log.d(LOG_TAG, "GeoPackage " + scStoreConfig.getUniqueID() + " already exists.  Not downloading.");
                // create new GeoPackage for the file that's already on disk
                gpkg = new GeoPackage(getContext(), scStoreConfig.getUniqueID());
                if (gpkg.isValid()) {
                    subscriber.onCompleted();
                }
                else {
                    Log.w(LOG_TAG, "GeoPackage was not valid, "+ gpkg.getName());
                    subscriber.onError(new Throwable("GeoPackage was not valid."));
                }
            }
            else {
                // download geopackage and store it locally
                URL theUrl = null;
                if (scStoreConfig.getUri().startsWith("http")) {
                    try {
                        theUrl = new URL(scStoreConfig.getUri());
                        download(theUrl.toString(), getContext().getDatabasePath(scStoreConfig.getUniqueID()))
                                .sample(2, TimeUnit.SECONDS)
                                .subscribe(
                                        new Action1<Float>() {
                                            @Override
                                            public void call(Float progress) {
                                                setDownloadProgress(progress);
                                                if (progress < 1) {
                                                    setStatus(SCDataStoreStatus.SC_DATA_STORE_DOWNLOADING_DATA);
                                                    subscriber.onNext(new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_DOWNLOADING_DATA));
                                                } else {
                                                    setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                                                    gpkg = new GeoPackage(getContext(), scStoreConfig.getUniqueID());
                                                    if (gpkg.isValid()) {
                                                        subscriber.onCompleted();
                                                    }
                                                    else {
                                                        Log.w(LOG_TAG, "GeoPackage was not valid, " + gpkg.getName());
                                                        subscriber.onError(new Throwable("GeoPackage was not valid."));
                                                    }
                                                }
                                            }
                                        },
                                        new Action1<Throwable>() {
                                            @Override
                                            public void call(Throwable t) {
                                                subscriber.onNext(new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_START_FAILED));
                                            }
                                        }
                                );
                    }
                    catch (MalformedURLException e) {
                        Log.e(LOG_TAG, "URL was malformed. Check the syntax: " + theUrl);
                        subscriber.onNext(new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_START_FAILED));
                        subscriber.onError(e);
                    }
                }
                else if (scStoreConfig.getUri().startsWith("file")) {
                    gpkg = new GeoPackage(getContext(), scStoreConfig.getUniqueID());
                    if (gpkg.isValid()) {
                        subscriber.onNext(new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_RUNNING));
                        subscriber.onCompleted();
                    }
                    else {
                        Log.w(LOG_TAG, "GeoPackage was not valid, "+ gpkg.getName());
                        subscriber.onNext(new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_START_FAILED));
                        subscriber.onError(new Throwable("GeoPackage was not valid."));
                    }
                }
            }
            }
        });


    }

    @Override
    public void stop() {
        this.setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void destroy() {
        deleteFile(getFilePath());
    }

    @Override
    public TileOverlay overlayFromLayer(String layer, GoogleMap map) {
        Map<String, SCGpkgTileSource> tileSources = getTileSources();
        if (tileSources.size() > 0 && tileSources.keySet().contains(layer)) {
            return map.addTileOverlay(
                    new TileOverlayOptions().tileProvider(
                            new GpkgTileProvider(tileSources.get(layer))
                    )
            );
        }
        return null;
    }

    @Override
    public SCPolygon getCoverage() {
        return null;
    }

    @Override
    public Observable<SCSpatialFeature> unSent() {
        return gpkg.unSent().map(new Func1<SCSpatialFeature, SCSpatialFeature>() {
            @Override
            public SCSpatialFeature call(SCSpatialFeature feature) {
                feature.setStoreId(storeId);
                return feature;
            }
        });
    }

    @Override
    public void updateAuditTable(SCSpatialFeature scSpatialFeature) {
        SCGpkgFeatureSource fs = gpkg.getFeatureSourceByName(scSpatialFeature.getLayerId());
        fs.updateAuditTable(scSpatialFeature);
    }

    @Override
    public String syncChannel() {
        return String.format(Locale.US, "/store/%s", this.storeId);
    }

    @Override
    public Map<String, Object> generateSendPayload(SCSpatialFeature scSpatialFeature) {
        return null;
    }

    private String createRtreeSubQuery(SCGpkgFeatureSource source, SCBoundingBox bbox) {
        return String.format("SELECT id FROM rtree_%s_%s WHERE minx > %f AND maxx < %f AND miny > %f AND maxy < %f",
                source.getTableName(),
                source.getGeomColumnName(),
                bbox.getMinX(),
                bbox.getMaxX(),
                bbox.getMinY(),
                bbox.getMaxY()
        );
    }

    private String getSelectColumnsString(SCGpkgFeatureSource featureSource) {
        StringBuilder sb = new StringBuilder();
        for (String columnName : featureSource.getColumns().keySet()) {
            sb.append(columnName).append(",");
        }
        sb.append(featureSource.getPrimaryKeyName()).append(",");
        String geomColumnName = featureSource.getGeomColumnName();
        sb.append("ST_AsBinary(").append(geomColumnName).append(") AS ").append(geomColumnName);
        return sb.toString();

    }

    private void saveFileToFilesystem(InputStream is) throws IOException {
        File dbFile = getContext().getDatabasePath(scStoreConfig.getUniqueID());
        FileOutputStream fos = new FileOutputStream(dbFile);
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = is.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        fos.close();
        Log.d(LOG_TAG, "Saved file to " + dbFile.getPath());
        Log.d(LOG_TAG, "Size of file in bytes " + dbFile.length());
    }

    private Func1<SqlBrite.Query, Observable<SCSpatialFeature>> getFeatureMapper(final SCGpkgFeatureSource source) {
        return new Func1<SqlBrite.Query, Observable<SCSpatialFeature>>() {

            @Override
            public Observable<SCSpatialFeature> call(SqlBrite.Query query) {
                return query.asRows(new Func1<Cursor, SCSpatialFeature>() {
                    @Override
                    public SCSpatialFeature call(final Cursor cursor) {
                        SCSpatialFeature feature = new SCSpatialFeature();
                        // deserialize byte[] to Geometry object
                        byte[] wkb = SCSqliteHelper.getBlob(cursor, source.getGeomColumnName());
                        try {
                            if (wkb != null && wkb.length > 0) {
                                feature = new SCGeometry(
                                        new WKBReader(GEOMETRY_FACTORY).read(wkb)
                                );
                            }
                        }
                        catch (ParseException e) {
                            Log.w(LOG_TAG, "Could not parse geometry");
                        }

                        final SCSpatialFeature finalFeature = feature;
                        finalFeature.setStoreId(scStoreConfig.getUniqueID());
                        finalFeature.setLayerId(source.getTableName());
                        finalFeature.setId(SCSqliteHelper.getString(cursor, source.getPrimaryKeyName()));

                        Observable<Map.Entry<String, String>> columns =
                                Observable.from(source.getColumns().entrySet())
                                .mergeWith(Observable.from(source.getGeometryColumns().entrySet()));

                        columns.subscribe(new Action1<Map.Entry<String, String>>() {
                            @Override
                            public void call(Map.Entry<String, String> column) {
                                if (column.getValue().equalsIgnoreCase("BLOB")
                                        || column.getValue().equalsIgnoreCase("GEOMETRY")
                                        || column.getValue().equalsIgnoreCase("POINT")
                                        || column.getValue().equalsIgnoreCase("LINESTRING")
                                        || column.getValue().equalsIgnoreCase("POLYGON")) {

                                    SCGeometry f = null;
                                    byte[] wkb = SCSqliteHelper.getBlob(cursor, column.getKey());
                                    try {
                                        if (wkb != null && wkb.length > 0) {
                                            f = new SCGeometry(
                                                    new WKBReader(GEOMETRY_FACTORY).read(wkb)
                                            );
                                        }
                                    }
                                    catch (ParseException e) {
                                        Log.w(LOG_TAG, "Could not parse geometry");
                                    }

                                    finalFeature.getProperties().put(
                                            column.getKey(),f
                                    );
                                }
                                else if (column.getValue().startsWith("INTEGER")) {
                                    finalFeature.getProperties().put(
                                            column.getKey(),
                                            SCSqliteHelper.getInt(cursor, column.getKey())
                                    );
                                }
                                else if (column.getValue().startsWith("REAL")) {
                                    finalFeature.getProperties().put(
                                            column.getKey(),
                                            SCSqliteHelper.getLong(cursor, column.getKey())
                                    );
                                }
                                else if (column.getValue().startsWith("TEXT")) {
                                    finalFeature.getProperties().put(
                                            column.getKey(),
                                            SCSqliteHelper.getString(cursor, column.getKey())
                                    );
                                }
                                else {
                                    Log.w(LOG_TAG, "The column type " + column.getValue() + " did not match any supported" +
                                            " column type so it wasn't added to the feature.");
                                }
                            }
                        });
//                        for (Map.Entry<String, String> column : source.getColumns().entrySet()) {
//                            if (column.getValue().equalsIgnoreCase("BLOB")
//                                    || column.getValue().equalsIgnoreCase("GEOMETRY")
//                                    || column.getValue().equalsIgnoreCase("POINT")
//                                    || column.getValue().equalsIgnoreCase("LINESTRING")
//                                    || column.getValue().equalsIgnoreCase("POLYGON")) {
//                                feature.getProperties().put(
//                                        column.getKey(),
//                                        SCSqliteHelper.getBlob(cursor, column.getKey())
//                                );
//                            }
//                            else if (column.getValue().startsWith("INTEGER")) {
//                                feature.getProperties().put(
//                                        column.getKey(),
//                                        SCSqliteHelper.getInt(cursor, column.getKey())
//                                );
//                            }
//                            else if (column.getValue().startsWith("REAL")) {
//                                feature.getProperties().put(
//                                        column.getKey(),
//                                        SCSqliteHelper.getLong(cursor, column.getKey())
//                                );
//                            }
//                            else if (column.getValue().startsWith("TEXT")) {
//                                feature.getProperties().put(
//                                        column.getKey(),
//                                        SCSqliteHelper.getString(cursor, column.getKey())
//                                );
//                            }
//                            else {
//                                Log.w(LOG_TAG, "The column type " + column.getValue() + " did not match any supported" +
//                                        " column type so it wasn't added to the feature.");
//                            }
//                        }
                        return finalFeature;
                    }
                });
            }
        };
    }

    public static String getVersionKey() {
        return String.format("%s.%s",TYPE, VERSION);
    }

    private boolean isGeometry(String type) {
        if (type.equalsIgnoreCase("GEOMETRY")) {
            return true;
        } else {
            return false;
        }
    }
}
