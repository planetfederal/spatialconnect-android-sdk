/**
 * Copyright 2015-2016 Boundless, http://boundlessgeo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.dataAdapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.db.GeoPackage;
import com.boundlessgeo.spatialconnect.db.GeoPackageContents;
import com.boundlessgeo.spatialconnect.db.SCGpkgFeatureSource;
import com.boundlessgeo.spatialconnect.tiles.GpkgTileProvider;
import com.boundlessgeo.spatialconnect.tiles.SCGpkgTileSource;
import com.boundlessgeo.spatialconnect.db.SCSqliteHelper;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreException;
import com.boundlessgeo.spatialconnect.stores.SCKeyTuple;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * This adpater connects to a specific GeoPackage defined by a {@link SCStoreConfig}.  The adapter will check to see if
 * the GeoPackage file already exists on disk and if not, it will attempt to download it.  After successfully
 * obtaining the file, it then creates a {@link GeoPackage} instance for interacting with that file.
 *
 * The adapter also provides the CRUD implementation to the {@link GeoPackageStore}.
 */
public class GeoPackageAdapter extends SCDataAdapter {

    private static final String NAME = "GeoPackageAdapter";
    private static final String TYPE = "gpkg";
    private static final int VERSION = 1;
    private Context context;
    private final String LOG_TAG = GeoPackageAdapter.class.getSimpleName();
    protected GeoPackage gpkg;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();


    public GeoPackageAdapter(Context context, SCStoreConfig scStoreConfig) {
        super(NAME, TYPE, VERSION);
        this.context = context;
        this.scStoreConfig = scStoreConfig;
    }

    @Override
    public Observable<SCDataAdapterStatus> connect() {
        final GeoPackageAdapter adapterInstance = this;
        Log.d(LOG_TAG, "Connecting adapter for store " + scStoreConfig.getName());

        return Observable.create(new Observable.OnSubscribe<SCDataAdapterStatus>() {

            @Override
            public void call(final Subscriber<? super SCDataAdapterStatus> subscriber) {

                Log.d(LOG_TAG, "Connecting to " + context.getDatabasePath(scStoreConfig.getUniqueID()).getPath());
                adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTING);

                // The db name on disk is its store ID to guarantee uniqueness on disk
                if (context.getDatabasePath(scStoreConfig.getUniqueID()).exists()) {
                    Log.d(LOG_TAG, "GeoPackage " + scStoreConfig.getUniqueID() + " already exists.  Not downloading.");
                    // create new GeoPackage for the file that's already on disk
                    gpkg = new GeoPackage(context, scStoreConfig.getUniqueID());
                    if (gpkg.isValid()) {
                        adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED);
                        subscriber.onCompleted();
                    }
                    else {
                        Log.w(LOG_TAG, "GeoPackage was not valid, "+ gpkg.getName());
                        adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
                        subscriber.onError(new Throwable("GeoPackage was not valid."));
                    }
                }
                else {
                    // download geopackage and store it locally
                    URL theUrl = null;
                    if (scStoreConfig.getUri().startsWith("http")) {
                        try {
                            theUrl = new URL(scStoreConfig.getUri());
                            downloadGeoPackage(theUrl)
                                    .subscribe(new Action1<Response>() {
                                        @Override
                                        public void call(Response response) {
                                            if (!response.isSuccessful()) {
                                                adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
                                                subscriber.onError(new IOException("Unexpected code " + response));
                                            }
                                            try {
                                                // save the file to the databases directory
                                                saveFileToFilesystem(response.body().byteStream());
                                                // create new GeoPackage instance now that we've saved the file
                                                gpkg = new GeoPackage(context, scStoreConfig.getUniqueID());
                                                if (gpkg.isValid()) {
                                                    adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED);
                                                    subscriber.onCompleted();
                                                }
                                                else {
                                                    Log.w(LOG_TAG, "GeoPackage was not valid, " + gpkg.getName());
                                                    adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
                                                    subscriber.onError(new Throwable("GeoPackage was not valid."));
                                                }
                                            }
                                            catch (IOException e) {
                                                adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
                                                subscriber.onError(e);
                                            }
                                            finally {
                                                response.body().close();
                                            }
                                        }
                                    });
                        }
                        catch (MalformedURLException e) {
                            Log.e(LOG_TAG, "URL was malformed. Check the syntax: " + theUrl);
                            adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
                            subscriber.onError(e);
                        }
                    }
                    else if (scStoreConfig.getUri().startsWith("file")) {
                        gpkg = new GeoPackage(context, scStoreConfig.getUniqueID());
                        if (gpkg.isValid()) {
                            adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED);
                            subscriber.onCompleted();
                        }
                        else {
                            Log.w(LOG_TAG, "GeoPackage was not valid, "+ gpkg.getName());
                            adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
                            subscriber.onError(new Throwable("GeoPackage was not valid."));
                        }
                    }
                }
            }
        });
    }

    @Override
    public void disconnect() {
        gpkg.close();
    }

    public void addLayer(String layer, Map<String,String> typeDefs) {
        if (!layerExists(layer)) {
            Log.d(LOG_TAG, "Adding layer " + layer + " to " + gpkg.getName());
            final String tableName = layer;
            Cursor cursor = null;
            BriteDatabase.Transaction tx = gpkg.newTransaction();
            try {
                // first create the table 
                cursor = gpkg.query(createTableSQL(layer, typeDefs));
                cursor.moveToFirst(); // force query to execute
                // then add it to gpkg_contents and any other tables (gpkg_metadata, ,etc) 
                cursor = gpkg.query(addToGpkgContentsSQL(tableName));
                cursor.moveToFirst(); // force query to execute
                // add a geometry column to the table b/c we want to store where the package was submitted (if needed). 
                // also, note that this function will add the geometry to gpkg_geometry_columns, which has a foreign key 
                // constraint on the table name, which requires the table to exist in gpkg_contents 
                cursor = gpkg.query(String.format("SELECT AddGeometryColumn('%s', 'geom', 'Geometry', 4326)", tableName));
                cursor.moveToFirst(); // force query to execute
                tx.markSuccessful();
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.w(LOG_TAG, "Could not create table b/c " + ex.getMessage());
            }
            finally {
                tx.end();
                gpkg.refreshFeatureSources();
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    /**
     * Deletes layer
     *
     * @param tableName - the name of the table/layer to remove from the DEFAULT_STORE
     */
    public void deleteLayer(String tableName) {
        BriteDatabase.Transaction tx = gpkg.newTransaction();
        // first remove from gpkg_geometry_columns
        gpkg.removeFromGpkgGeometryColumns(tableName);
        // then remove it from geopackage contents
        gpkg.removeFromGpkgContents(tableName);
        // lastly, remove the table itself so there are no FK constraint violations
        gpkg.query("DROP TABLE " + tableName);
        tx.markSuccessful();
        tx.end();
        gpkg.refreshFeatureSources();
    }

    private boolean layerExists(String layer) {
        boolean layerExists = false;
        String table = layer;
        Cursor cursor = gpkg.query("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?",new String[]{table});
        try {
            if (cursor.moveToFirst()) {
                layerExists = cursor.getInt(0) > 0;
            }
        }
        finally {
            cursor.close();
        }

        return  layerExists;
    }


    /**
     * Query {@link SCDataStore}s for {@link SCSpatialFeature}s using a
     * {@link SCQueryFilter} to describe what should be returned. It's important to note that the Observable
     * returned from the this method does not complete, so subscribers are expected to unsubscribe when they are
     * finished.
     * <br/>
     * For each query, we use a <a href="https://en.wikipedia.org/wiki/Correlated_subquery">correlated sub query</a>
     * on the R*-tree index to optimize the query.
     *
     * @param queryFilter specifies the criteria needed to execute the query
     * @return
     */
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

    /**
     * Query for a feature by its {@link SCKeyTuple}.
     *
     * @param keyTuple
     * @return
     */
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
            return gpkg.createQuery(
                    tableName,
                    String.format(
                            "SELECT %s FROM %s WHERE %s = %s LIMIT 1",
                            getSelectColumnsString(featureSource),
                            tableName,
                            featureSource.getPrimaryKeyName(),
                            keyTuple.getFeatureId()
                    )
            ).flatMap(getFeatureMapper(featureSource));
        }
    }

    /**
     * Create a new {@link SCSpatialFeature} by persisting to the db and returning the object with its newly created id.
     *
     * @param scSpatialFeature
     * @return an observable of the created feature
     */
    public Observable<SCSpatialFeature> create(final SCSpatialFeature scSpatialFeature) {
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
                                String.format("INSERT OR REPLACE INTO %s (%s) VALUES (%s)",
                                        tableName,
                                        featureSource.getColumnNamesForInsert(scSpatialFeature),
                                        featureSource.getColumnValuesForInsert(scSpatialFeature)
                                )
                        );
                        // get the id of the last inserted row: https://www.sqlite.org/lang_corefunc.html#last_insert_rowid
                        Cursor cursor = gpkg.query("SELECT last_insert_rowid()");
                        if (cursor != null) {
                            cursor.moveToFirst();  // force query to execute
                            final Integer pk = cursor.getInt(0);
                            scSpatialFeature.setId(String.valueOf(pk));
                            subscriber.onNext(scSpatialFeature);
                            subscriber.onCompleted();
                        }
                    }
                    catch (SQLException ex) {
                        subscriber.onError(new Throwable("Could not create the the feature.", ex));
                    }
                }
            });
        }
    }

    /**
     * Update an {@link SCSpatialFeature} by persisting to the db and returning the updated object.
     *
     * @param scSpatialFeature the feature to update
     * @return an observable of the update feature
     */
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

    /**
     * Deletes an {@link SCSpatialFeature} and closes the observable if the delete was successful, otherwise it returns
     * and error.
     *
     * @param keyTuple the {@link SCKeyTuple} of the feature to delete
     * @return onCompleted if delete is successful, onError if not
     */
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



    public Set<GeoPackageContents> getGeoPackageContents() {
        return gpkg.getGeoPackageContents();
    }

    public Map<String, SCGpkgFeatureSource> getFeatureSources() {
        return gpkg.getFeatureSources();
    }

    public Map<String, SCGpkgTileSource> getTileSources() {
        return gpkg.getTileSources();
    }

    public void overlayFromLayer(String layer, GoogleMap mapView) {
        Map<String, SCGpkgTileSource> tileSources = getTileSources();
        if (tileSources.size() > 0) {
            if (tileSources.keySet().contains(layer)) {
                mapView.addTileOverlay(
                        new TileOverlayOptions().tileProvider(
                                new GpkgTileProvider(tileSources.get(layer))
                        )
                );
            }
        }
    }

    /**
     * Downloads a GeoPackage and returns a Response.
     *
     * @param theUrl
     * @return
     */
    public Observable<Response> downloadGeoPackage(final URL theUrl) {
        Log.d(LOG_TAG, "Attempting to download GeoPackage from " + theUrl.toString());
        try {
            return HttpHandler.getInstance().get(theUrl.toString());
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Could not download GeoPackage from " + theUrl.toString());
            return Observable.error(e);
        }
    }

    /**
     * Returns a function that can map all the rows from a query to stream of {@link SCSpatialFeature}s.
     *
     * @param source
     * @return
     */
    public Func1<SqlBrite.Query, Observable<SCSpatialFeature>> getFeatureMapper(final SCGpkgFeatureSource source) {
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
                            if (wkb != null || wkb.length > 0) {
                                feature = new SCGeometry(
                                        new WKBReader(GEOMETRY_FACTORY).read(wkb)
                                );
                            }
                        }
                        catch (ParseException e) {
                            Log.w(LOG_TAG, "Could not parse geometry");
                        }
                        feature.setStoreId(scStoreConfig.getUniqueID());
                        feature.setLayerId(source.getTableName());
                        feature.setId(SCSqliteHelper.getString(cursor, source.getPrimaryKeyName()));
                        for (Map.Entry<String, String> column : source.getColumns().entrySet()) {
                            if (column.getValue().equalsIgnoreCase("BLOB")
                                    || column.getValue().equalsIgnoreCase("GEOMETRY")
                                    || column.getValue().equalsIgnoreCase("POINT")
                                    || column.getValue().equalsIgnoreCase("LINESTRING")
                                    || column.getValue().equalsIgnoreCase("POLYGON")) {
                                feature.getProperties().put(
                                        column.getKey(),
                                        SCSqliteHelper.getBlob(cursor, column.getKey())
                                );
                            }
                            else if (column.getValue().startsWith("INTEGER")) {
                                feature.getProperties().put(
                                        column.getKey(),
                                        SCSqliteHelper.getInt(cursor, column.getKey())
                                );
                            }
                            else if (column.getValue().startsWith("REAL")) {
                                feature.getProperties().put(
                                        column.getKey(),
                                        SCSqliteHelper.getLong(cursor, column.getKey())
                                );
                            }
                            else if (column.getValue().startsWith("TEXT")) {
                                feature.getProperties().put(
                                        column.getKey(),
                                        SCSqliteHelper.getString(cursor, column.getKey())
                                );
                            }
                            else {
                                Log.w(LOG_TAG, "The column type " + column.getValue() + " did not match any supported" +
                                        " column type so it wasn't added to the feature.");
                            }
                        }
                        return feature;
                    }
                });
            }
        };
    }


    /**
     * Populate a {@link ContentValues} for all of the columns excluding the primary key and geometry columns.
     *
     * @param scSpatialFeature
     * @return
     */
    private ContentValues getContentValues(SCSpatialFeature scSpatialFeature) {
        ContentValues values = new ContentValues();
        String tableName = scSpatialFeature.getKey().getLayerId();
        SCGpkgFeatureSource featureSource = gpkg.getFeatureSourceByName(tableName);
        Map<String, String> columns = featureSource.getColumns();
        for (Map.Entry<String, Object> prop : scSpatialFeature.getProperties().entrySet()) {
            String columnType = columns.get(prop.getKey());
            if (columnType.startsWith("TEXT")) {
                values.put(prop.getKey(), (String) prop.getValue());
            }
            else if (columnType.startsWith("BLOB")
                    || columnType.equalsIgnoreCase("GEOMETRY")
                    || columnType.equalsIgnoreCase("POINT")
                    || columnType.equalsIgnoreCase("LINESTRING")
                    || columnType.equalsIgnoreCase("POLYGON")) {
                values.put(prop.getKey(), (byte[]) prop.getValue());
            }
            else if (columnType.startsWith("INTEGER")) {
                values.put(prop.getKey(), (Integer) prop.getValue());
            }
            else if (columnType.startsWith("REAL")) {
                values.put(prop.getKey(), (Double) prop.getValue());
            }
            else {
                Log.w(LOG_TAG, "The column type " + columnType + " did not match any supported column type.");
            }
        }
        return values;
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
        File dbFile = context.getDatabasePath(scStoreConfig.getUniqueID());
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

    private String createTableSQL(String layer, Map<String, String> typeDefs){
        final String tableName = layer;
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName);
        sb.append(" (id INTEGER PRIMARY KEY AUTOINCREMENT ");
        for (String key : typeDefs.keySet()) {
            sb.append(", ");
            sb.append(key);
            sb.append(" ");
            sb.append(typeDefs.get(key));
        }
        sb.append(")");

        return sb.toString();
    }

    private String addToGpkgContentsSQL(String tableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT OR REPLACE INTO gpkg_contents ")
                .append("(table_name,data_type,identifier,description,min_x,min_y,max_x,max_y,srs_id) ")
                .append(String.format("VALUES ('%s','features','%s','%s',0,0,0,0,4326)", tableName, tableName, tableName));
        return sb.toString();
    }

}
