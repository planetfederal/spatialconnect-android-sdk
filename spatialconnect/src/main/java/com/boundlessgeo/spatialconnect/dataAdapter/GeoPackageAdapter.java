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

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCFormField;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.db.GeoPackage;
import com.boundlessgeo.spatialconnect.db.GeoPackageContents;
import com.boundlessgeo.spatialconnect.db.SCGpkgFeatureSource;
import com.boundlessgeo.spatialconnect.db.SCSqliteHelper;
import com.boundlessgeo.spatialconnect.db.SQLiteType;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.services.SCConfigService;
import com.boundlessgeo.spatialconnect.services.SCNetworkService;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreException;
import com.boundlessgeo.spatialconnect.stores.SCKeyTuple;
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

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

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

                Log.d(LOG_TAG, "Connecting to " + context.getDatabasePath(scStoreConfig.getName()).getPath());
                adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTING);

                if (context.getDatabasePath(scStoreConfig.getName()).exists()) {
                    Log.d(LOG_TAG, "GeoPackage " + scStoreConfig.getName() + " already exists.  Not downloading.");
                    // create new GeoPackage for the file that's already on disk
                    gpkg = new GeoPackage(context, scStoreConfig.getName());
                    adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED);
                    subscriber.onCompleted();
                }
                else {
                    // download geopackage and store it locally
                    URL theUrl = null;
                    if (scStoreConfig.getUri().startsWith("http")) {
                        try {
                            theUrl = new URL(scStoreConfig.getUri());
                            downloadGeoPackage(theUrl)
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(new Action1<Response>() {
                                        @Override
                                        public void call(Response response) {
                                            if (!response.isSuccessful()) {
                                                subscriber.onError(new IOException("Unexpected code " + response));
                                            }
                                            try {
                                                // save the file to the databases directory
                                                saveFileToFilesystem(response.body().byteStream());
                                                // create new GeoPackage now that we've saved the file
                                                gpkg = new GeoPackage(context, scStoreConfig.getName());
                                                adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED);
                                                subscriber.onCompleted();
                                            }
                                            catch (IOException e) {
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
                        gpkg = new GeoPackage(context, scStoreConfig.getName());
                        adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED);
                        subscriber.onCompleted();
                    }
                }
            }
        });
    }

    /**
     * Creates a new table for the form specified in the formConfig.
     *
     * @param formConfig
     */
    public void addFormLayer(SCFormConfig formConfig) {
        if (!formLayerExists(formConfig)) {
            Log.d(LOG_TAG, "Creating a table for form " + formConfig.getFormKey());
            final String tableName = formConfig.getFormKey();
            Cursor cursor = null;
            BriteDatabase.Transaction tx = gpkg.newTransaction();
            try {
                // first create the table for this form
                cursor = gpkg.query(createFormTableSQL(formConfig));
                cursor.moveToFirst(); // force query to execute
                // then add it to gpkg_contents and any other tables (gpkg_metadata, ,etc)
                cursor = gpkg.query(addToGpkgContentsSQL(tableName));
                cursor.moveToFirst(); // force query to execute
                // add a geometry column to the table b/c we want to store where the form was submitted (if needed).
                // also, note that this function will add the geometry to gpkg_geometry_columns, which has a foreign key
                // constraint on the table name, which requires the table to exist in gpkg_contents
                cursor = gpkg.query(String.format("SELECT AddGeometryColumn('%s', 'geom', 'Geometry', 4326)", tableName));
                cursor.moveToFirst(); // force query to execute
                tx.markSuccessful();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                Log.w(LOG_TAG, "Could not create table b/c " + ex.getMessage());
            }
            finally {
                tx.end();
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private boolean formLayerExists(SCFormConfig formConfig) {
        boolean formLayerExists = false;
        String formTableName = formConfig.getFormKey();
        Cursor cursor = gpkg.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{formTableName}
        );
        try {
            if (cursor.moveToFirst()) {
                formLayerExists = cursor.getInt(0) > 0;
            }
        }
        finally {
            cursor.close();
        }
        return formLayerExists;
    }

    /**
     * Deletes a form table.
     *
     * @param tableName - the name of the table/layer to remove from the DEFAULT_STORE
     */
    public void deleteFormLayer(String tableName) {
        BriteDatabase.Transaction tx = gpkg.newTransaction();
        // first remove from gpkg_geometry_columns
        gpkg.removeFromGpkgGeometryColumns(tableName);
        // then remove it from geopackage contents
        gpkg.removeFromGpkgContents(tableName);
        // lastly, remove the table itself so there are no FK constraint violations
        gpkg.query("DROP TABLE " + tableName);
        tx.markSuccessful();
        tx.end();
    }

    private String createFormTableSQL(SCFormConfig formConfig){
        final String tableName = formConfig.getFormKey();
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName);
        sb.append(" (id INTEGER PRIMARY KEY AUTOINCREMENT, ");
        boolean isFirst = true;
        for (SCFormField field : formConfig.getFields()) {
            if (!isFirst) {
                sb.append(", ");
            }
            sb.append(field.getKey().replace(" ", "_").toLowerCase());
            sb.append(" ").append(field.getColumnType());
            isFirst = false;
        }
        sb.append(")");
        return sb.toString();
    }

    private String addToGpkgContentsSQL(String tableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT OR REPLACE INTO gpkg_contents ")
                .append("(table_name,data_type,identifier,description,min_x,min_y,max_x,max_y,srs_id) ")
                .append(String.format("VALUES ('%s','features','%s','form',0,0,0,0,4326)", tableName, tableName));
        return sb.toString();
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
        final List<SCGpkgFeatureSource> sources = getFeatureSources();
        // if there are no layer names supplied in the query filter, then search on all tables
        List<String> layerNames = queryFilter.getLayerIds();
        if (layerNames.size() == 0) {
            layerNames = getFeatureSourceNames();
        }
        return Observable.from(layerNames)
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String layer) {
                        return sources.contains(getFeatureSourceByName(layer));
                    }
                })
                .flatMap(new Func1<String, Observable<SCSpatialFeature>>() {
                    @Override
                    public Observable<SCSpatialFeature> call(final String layerName) {
                        Log.d(LOG_TAG, "Querying on layer " + layerName);
                        final SCGpkgFeatureSource featureSource = getFeatureSourceByName(layerName);
                        return gpkg.createQuery(
                                layerName,
                                String.format(
                                        "SELECT %s FROM %s WHERE %s IN (%s) LIMIT %d",
                                        getSelectColumnsString(featureSource),
                                        layerName,
                                        featureSource.getPrimaryKeyName(),
                                        createRtreeSubQuery(featureSource, queryFilter.getPredicate().getBoundingBox()),
                                        queryFilter.getLimit() / getGeoPackageContents().size()
                                )
                        ).flatMap(getFeatureMapper(featureSource)).onBackpressureBuffer(queryFilter.getLimit());
                    }
                });
    }

    /**
     * Query for a feature by its {@link SCKeyTuple}.
     *
     * @param keyTuple
     * @return
     */
    public Observable<SCSpatialFeature> queryById(final SCKeyTuple keyTuple) {
        final String tableName = keyTuple.getLayerId();
        final SCGpkgFeatureSource featureSource = getFeatureSourceByName(tableName);
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
                        for (Map.Entry<String, SQLiteType> column : source.getColumns().entrySet()) {
                            switch (column.getValue()) {
                                case BLOB:
                                    feature.getProperties().put(
                                            column.getKey(),
                                            SCSqliteHelper.getBlob(cursor, column.getKey())
                                    );
                                    break;
                                case INTEGER:
                                    feature.getProperties().put(
                                            column.getKey(),
                                            SCSqliteHelper.getInt(cursor, column.getKey())
                                    );
                                    break;
                                case REAL:
                                    feature.getProperties().put(
                                            column.getKey(),
                                            SCSqliteHelper.getLong(cursor, column.getKey())
                                    );
                                    break;
                                case TEXT:
                                    feature.getProperties().put(
                                            column.getKey(),
                                            SCSqliteHelper.getString(cursor, column.getKey())
                                    );
                                    break;
                            }
                        }
                        return feature;
                    }
                });
            }
        };
    }

    /**
     * Create a new {@link SCSpatialFeature} by persisting to the db and returning the object with its newly created id.
     *
     * @param scSpatialFeature
     * @return an observable of the created feature
     */
    public Observable<SCSpatialFeature> create(final SCSpatialFeature scSpatialFeature) {
        final String tableName = scSpatialFeature.getKey().getLayerId();
        final SCGpkgFeatureSource featureSource = getFeatureSourceByName(tableName);
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
                            // queue feature to be posted to backend
                            postCreatedFeature(scSpatialFeature);
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

    private void postCreatedFeature(SCSpatialFeature feature) {
        String formId = "";
        for(SCFormConfig config : SpatialConnect.getInstance().getDataService().getDefaultStore().getFormConfigs()) {
            if (config.getFormKey().equals(feature.getKey().getLayerId())) {
                formId = config.getId();
            }
        }
        if (formId != null) {
            final String theUrl = SCConfigService.API_URL + "forms/" + formId + "/submit";
            Log.d(LOG_TAG, "Posting created feature to " + theUrl);
            SCNetworkService networkService = SpatialConnect.getInstance().getNetworkService();
            String response = null;
            try {
                response = networkService.post(theUrl, feature.toJson());
                Log.d(LOG_TAG, "create new feature response " + response);
            }
            catch (IOException e) {
                Log.w(LOG_TAG, "could not create new feature on backend");
            }
        }
        else {
            Log.w(LOG_TAG, "Could not post feature b/c form id was null");
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
        final SCGpkgFeatureSource featureSource = getFeatureSourceByName(tableName);
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
        final SCGpkgFeatureSource featureSource = getFeatureSourceByName(tableName);
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
    public void disconnect() {
        gpkg.close();
    }

    public List<GeoPackageContents> getGeoPackageContents() {
        return gpkg.getGeoPackageContents().toBlocking().first();
    }

    public List<SCGpkgFeatureSource> getFeatureSources() {
        return gpkg.getFeatureSources().toBlocking().first();
    }

    private List<String> getFeatureSourceNames() {
        List<SCGpkgFeatureSource> featureSources = gpkg.getFeatureSources().toBlocking().first();
        ArrayList<String> names = new ArrayList<>(featureSources.size());
        for (SCGpkgFeatureSource source : featureSources) {
            names.add(source.getTableName());
        }
        return names;
    }


    /**
     * Downloads a GeoPackage and returns a Response.
     *
     * @param theUrl
     * @return
     */
    public Observable<Response> downloadGeoPackage(URL theUrl) {
        Log.d(LOG_TAG, "Downloading GeoPackage from " + theUrl.toString());

        Request request = new Request.Builder()
                .url(theUrl)
                .build();

        // TODO: should this be an http utility or part of the network service instead?
        final ProgressListener progressListener = new ProgressListener() {
            @Override
            public void update(long bytesRead, long contentLength, boolean done) {
                // TODO: consider wrapping the  update in an observable that you can throttle
                // something like this: https://groups.google.com/forum/#!msg/rxjava/aqDM7Eq3zT8/PCF_7pdlGgAJ
//                Log.v(LOG_TAG, String.format("%d%% done\n", (100 * bytesRead) / contentLength));
            }
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Response originalResponse = chain.proceed(chain.request());
                        return originalResponse.newBuilder()
                                .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                                .build();
                    }
                })
                .build();

        try {
            return Observable.just(client.newCall(request).execute());
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Could not download GeoPackage from " + theUrl.toString());
            return Observable.error(e);
        }

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
        SCGpkgFeatureSource featureSource = getFeatureSourceByName(tableName);
        Map<String, SQLiteType> columns = featureSource.getColumns();
        for (Map.Entry<String, Object> prop : scSpatialFeature.getProperties().entrySet()) {
            if (columns.get(prop.getKey()).equals(SQLiteType.TEXT)) {
                values.put(prop.getKey(), (String) prop.getValue());
            }
            else if (columns.get(prop.getKey()).equals(SQLiteType.BLOB)) {
                values.put(prop.getKey(), (byte[]) prop.getValue());
            }
            else if (columns.get(prop.getKey()).equals(SQLiteType.INTEGER)) {
                values.put(prop.getKey(), (Integer) prop.getValue());
            }
            else if (columns.get(prop.getKey()).equals(SQLiteType.REAL)) {
                values.put(prop.getKey(), (Double) prop.getValue());
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

    private SCGpkgFeatureSource getFeatureSourceByName(String name) {
        SCGpkgFeatureSource scGpkgFeatureSource = null;
        for (SCGpkgFeatureSource source : getFeatureSources()) {
            if (source.getTableName().equals(name)) {
                scGpkgFeatureSource = source;
            }
        }
        return scGpkgFeatureSource;
    }

    private void saveFileToFilesystem(InputStream is) throws IOException {
        File dbFile = context.getDatabasePath(scStoreConfig.getName());
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

    // from https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/Progress.java
    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        public ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }

    interface ProgressListener {
        void update(long bytesRead, long contentLength, boolean done);
    }

}
