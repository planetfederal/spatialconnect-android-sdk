package com.boundlessgeo.spatialconnect.stores;


import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;
import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageConstants;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.tiles.overlay.GeoPackageOverlayFactory;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.wkb.io.ByteReader;
import mil.nga.wkb.io.ByteWriter;
import mil.nga.wkb.io.WkbGeometryReader;
import mil.nga.wkb.io.WkbGeometryWriter;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Provides capabilities for interacting with a single GeoPackage.
 */
public class GeoPackageStore extends SCDataStore {

    private static final String STORE_NAME = "GeoPackageStore";
    private static final String LOG_TAG = GeoPackageStore.class.getSimpleName();
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    private static String TYPE = "geopackage";
    private static int VERSION = 1;
    private static final int DEFUALT_FEATURE_LIMIT = 100;

    public static String versionKey() {
        return TYPE + "." + VERSION;
    }

    /**
     * Constructor for GeoPackageStore that initializes the data store adapter
     * based on the scStoreConfig.
     *
     * @param context
     *         instance of the current activity's context
     * @param scStoreConfig
     *         instance of the configuration needed to configure the store
     */
    public GeoPackageStore(Context context, SCStoreConfig scStoreConfig) {
        super(context, scStoreConfig);
        // TODO: revist if we want to have setters
        this.setName(scStoreConfig.getName());
        this.setType(TYPE);
        this.setVersion(VERSION);
        this.getKey();
        // setup the adapter for this store
        GeoPackageAdapter adapter = new GeoPackageAdapter(context, scStoreConfig);
        this.setAdapter(adapter);
    }

    @Override
    public DataStorePermissionEnum getAuthorization() {
        return DataStorePermissionEnum.READ_WRITE;
    }

    @Override
    public Observable<SCSpatialFeature> query(final SCQueryFilter scFilter) {
        final GeoPackageAdapter adapter = (GeoPackageAdapter) this.getAdapter();
        final GeoPackageManager manager = adapter.getGeoPackageManager();
        int numberOfLayers = manager.open(adapter.getDataStoreName()).getFeatureTables().size();

        final int featuresPerStoreLimit = Math.round(DEFUALT_FEATURE_LIMIT / numberOfLayers);

        // create a stream for the geopackage database name of this store
        Observable<String> databaseStream = Observable.just(adapter.getDataStoreName());

        // create a stream of geopackage
        Observable<GeoPackage> geoPackageStream = databaseStream.flatMap(
                new Func1<String, Observable<GeoPackage>>() {
                    @Override
                    public Observable<GeoPackage> call(String geopackageName) {
                        return Observable.just(manager.open(geopackageName));
                    }
                }
        );

        Observable<FeatureCursor> featureCursorStream = geoPackageStream.flatMap(
                new Func1<GeoPackage, Observable<FeatureCursor>>() {
                    @Override
                    public Observable<FeatureCursor> call(final GeoPackage geoPackage) {

                        return Observable.create(new Observable.OnSubscribe<FeatureCursor>() {
                            @Override
                            public void call(Subscriber<? super FeatureCursor> subscriber) {

                                for (String featureTableName : geoPackage.getFeatureTables()) {
                                    if (geoPackage.getFeatureTables().contains(featureTableName)) {
                                        FeatureDao featureDao = geoPackage.getFeatureDao(featureTableName);
                                        try {
                                            FeatureCursor fc = featureDao.queryForAll();
                                            subscriber.onNext(fc);
                                        } catch (Exception e) {
                                            Log.e(LOG_TAG, "Couldn't send next feature");
                                            subscriber.onError(e);
                                        }
                                    }
                                }
                                subscriber.onCompleted();
                            }
                        });
                    }
                }
        );


        // return the stream of matching features
        return featureCursorStream.flatMap(
                new Func1<FeatureCursor, Observable<SCSpatialFeature>>() {
                    @Override
                    public Observable<SCSpatialFeature> call(final FeatureCursor featureCursor) {
                        // create a new observable from scratch to emit features (SCGeometrys)
                        return Observable.create(new Observable.OnSubscribe<SCSpatialFeature>() {
                            @Override
                            public void call(Subscriber<? super SCSpatialFeature> subscriber) {
                                try {
                                    int featuresCount = 0;
                                    while (featureCursor != null && !featureCursor.isClosed()
                                            && featureCursor.moveToNext() && featuresCount < featuresPerStoreLimit) {
                                        try {
                                            SCSpatialFeature feature = createSCSpatialFeature(featureCursor.getRow());
                                            if (feature instanceof SCGeometry &&
                                                    ((SCGeometry) feature).getGeometry() != null &&
                                                    scFilter.getPredicate().isInBoundingBox((SCGeometry) feature)) {
                                                featuresCount++;
                                                subscriber.onNext(feature);
                                            }
                                        } catch (ParseException e) {
                                            Log.w(LOG_TAG, "Couldn't parse the geometry.");
                                            subscriber.onError(e);
                                        } catch (Exception e) {
                                            Log.e(LOG_TAG, "Unexpected exception.");
                                            subscriber.onError(e);
                                        }
                                    }
                                } finally {
                                    featureCursor.close();
                                    subscriber.onCompleted();
                                }
                            }
                        }).onBackpressureBuffer();  // this is needed otherwise the filter can't keep up and will
                        // throw MissingBackpressureException
                    }
                });
    }

    @Override
    public Observable<Boolean> create(final SCSpatialFeature scSpatialFeature) {

        // for now we'll only allow writing to the point_features table
        // TODO: reafactor this when refactoring away from id as store.layer.featureid
        final GeoPackageManager manager = ((GeoPackageAdapter) this.getAdapter()).getGeoPackageManager();
        final GeoPackage geoPackage = manager.open(this.getAdapter().getDataStoreName());
        final FeatureDao featureDao = geoPackage.getFeatureDao("point_features");
        final FeatureRow newRow = featureDao.newRow();

        // populate the new row with feature data
        Geometry geom = ((SCGeometry) scSpatialFeature).getGeometry();
        if (geom != null) {
          try {
            GeoPackageGeometryData geometryData = new GeoPackageGeometryData(getStandardGeoPackageBinary(geom));
            newRow.setGeometry(geometryData);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        // add all the properties to the matching columns
        for (String key : scSpatialFeature.getProperties().keySet()) {
          if (Arrays.asList(featureDao.getTable().getColumnNames()).contains(key)) {
            if (!key.equals("id")) {
              newRow.setValue(key, scSpatialFeature.getProperties().get(key));
            }
          }
        }

        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                   featureDao.create(newRow);
                    subscriber.onNext(Boolean.TRUE);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Could not create feature with id " + scSpatialFeature.getId());
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    // something should subscribe to this and update the contents table with the last_change
    public Observable<Boolean> update(final SCSpatialFeature scSpatialFeature) {

        final FeatureRow rowToUpdate = toFeatureRow(scSpatialFeature);

        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    if (rowToUpdate != null &&
                            getFeatureDao(scSpatialFeature.getId()).update(rowToUpdate) == 1) {
                        subscriber.onNext(Boolean.TRUE);
                    } else {
                        subscriber.onNext(Boolean.FALSE);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Could not update feature with id " + scSpatialFeature.getId());
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }

        });
    }

    @Override
    public Observable<Boolean> delete(final SCKeyTuple tuple) {

        final String featureId = tuple.getFeatureId();

        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    Long rowId = Long.valueOf(getRowId(featureId));
                    if (getFeatureDao(featureId).deleteById(rowId) == 1) {
                        subscriber.onNext(Boolean.TRUE);
                    } else {
                        Log.w(LOG_TAG, "Expected 1 row to have been deleted for feature with id "
                                + featureId);
                        subscriber.onNext(Boolean.FALSE);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Could not create delete with id " + featureId);
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<SCStoreStatusEvent> start() {
        final String storeId = this.getStoreId();
        final GeoPackageStore storeInstance = this;

        return Observable.create(new Observable.OnSubscribe<SCStoreStatusEvent>() {
          @Override
          public void call(final Subscriber<? super SCStoreStatusEvent> subscriber) {

            // subscribe to an Observable/stream that lets us know when the adapter is connected or disconnected
            storeInstance.getAdapter().connect().subscribe(new Subscriber<SCDataAdapterStatus>() {

                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    Log.w(LOG_TAG, "Could not activate data store " + storeId);
                    storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
                    subscriber.onError(new Exception("Could not activate data store " + storeId));
                }

                @Override
                public void onNext(SCDataAdapterStatus status) {
                    if (status.equals(SCDataAdapterStatus.DATA_ADAPTER_CONNECTING)) {
                        Log.w(LOG_TAG, "Data Store " + storeId + " started to connect");
                        storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                        subscriber.onNext(new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_STARTED, storeId));
                    }
                    if (status.equals(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED)) {
                        Log.w(LOG_TAG, "Data Store " + storeId + " is now running");
                        storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                        subscriber.onNext(new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_RUNNING, storeId));
                    }
                }
            });
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

    /**
     * Sets a GoogleMap's TileOverlay to use a TileProvider that uses tiles stored in the GeoPackage
     *
     * @param map
     * @param geopackageName
     * @param tileTableName
     */
    public void addGeoPackageTileOverlay(GoogleMap map, String geopackageName, String tileTableName) {
        final GeoPackageAdapter adapter = (GeoPackageAdapter) this.getAdapter();
        final GeoPackageManager manager = adapter.getGeoPackageManager();
        GeoPackage geoPackage = manager.open(geopackageName);
        TileDao tileDao = geoPackage.getTileDao(tileTableName);
        TileProvider overlay = GeoPackageOverlayFactory.getTileProvider(tileDao);
        TileOverlayOptions overlayOptions = new TileOverlayOptions();
        overlayOptions.tileProvider(overlay);
        overlayOptions.zIndex(-1);
        map.addTileOverlay(overlayOptions);
    }


    /**
     * Creates an SCSpatialFeature instance from a FeatureRow.
     *
     * @param row
     *         a populated FeatureRow instance
     *
     * @return SCSpatialFeature instance populated with the data from the FeatureRow
     *
     * @throws ParseException
     */
    protected SCSpatialFeature createSCSpatialFeature(final FeatureRow row) throws ParseException {
        SCSpatialFeature scSpatialFeature;
        // set the geometry's geometry
        GeoPackageGeometryData geometryData = row.getGeometry();
        if (geometryData != null) {
            scSpatialFeature = new SCGeometry(
                    new WKBReader(geometryFactory).read(geometryData.getWkbBytes())
            );
        } else {
            scSpatialFeature = new SCSpatialFeature();
        }
        // set the id as storeId.featureTableName.idOfRow
        scSpatialFeature.setId(String.format("%s.%s.%s",
                        this.getStoreId(),
                        row.getTable().getTableName(),
                        Long.toString(row.getId()))
        );
        // populate properties map with data from each column
        Map<String, Object> props = scSpatialFeature.getProperties();
        for (String columnName : row.getColumnNames()) {
            if (null != row.getValue(columnName) && !row.getColumn(columnName).isGeometry()) {
                props.put(columnName, row.getValue(columnName));
            }
        }
        // populate the layer and store id
        scSpatialFeature.setLayerId(row.getTable().getTableName());
        scSpatialFeature.setStoreId(this.getStoreId());
        return scSpatialFeature;
    }

    /**
     * Helper method that returns a new FeatureRow instance based on the properties of an
     * SCSptaialFeature instance.  If the SCSpatialFeature instance represents an existing
     * FeatureRow, then that row is used, otherwise a new instance is created.
     *
     * Note that it is not possible to set the id of a new FeatureRow because the
     * <a href="http://www.geopackage.org/spec/#requirement_feature_integer_pk">
     * GeoPackage specification</a> requires that the id is auto incremented.  Also, when
     * constructing the FeatureRow, this method will ignore any properties of the SCSpatialFeature
     * instance that do not already have a corresponding column in the feature table.
     *
     * @param scSpatialFeature
     *         instance of an SCSpatialFeature that will be converted to a new
     *         FeatureRow instance.
     *
     * @return a FeatureRow instance populated with all the properties from the scSpatialFeature
     */
    protected FeatureRow toFeatureRow(final SCSpatialFeature scSpatialFeature) {
        final String featureId = scSpatialFeature.getId();
        final FeatureDao featureDao = getFeatureDao(featureId);

        FeatureRow featureRow = featureDao.queryForIdRow(Long.valueOf(getRowId(featureId)));
        if (featureRow == null) {
            featureRow = featureDao.newRow();
        }

        // add all the properties to the matching columns
        for (String key : scSpatialFeature.getProperties().keySet()) {
            if (Arrays.asList(featureDao.getTable().getColumnNames()).contains(key)) {
                if (!key.equals(featureRow.getPkColumn().getName())) {
                    featureRow.setValue(key, scSpatialFeature.getProperties().get(key));
                }
            }
        }

        // add the geometry to the row if needed
        if (scSpatialFeature instanceof SCGeometry) {
            Geometry geom = ((SCGeometry) scSpatialFeature).getGeometry();
            if (geom != null) {
              try {
                GeoPackageGeometryData geometryData = new GeoPackageGeometryData(getStandardGeoPackageBinary(geom));
                featureRow.setGeometry(geometryData);
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
        }

        return featureRow;
    }

    /**
     * Helper method that returns the FeatureDao instance based on the feature table name found
     * within the scSpatialFeature's id.
     *
     * @param featureId
     *         the feature id of the SCSpatialFeature instance
     *
     * @return the instance of the FeatureDao used to interact with the feature table that the
     * feature belongs to
     */
    public FeatureDao getFeatureDao(String featureId) {
        GeoPackageManager manager = ((GeoPackageAdapter) this.getAdapter()).getGeoPackageManager();
        GeoPackage geoPackage = manager.open(this.getAdapter().getDataStoreName());
        // TODO: what do we do if we can't parse the a table name???
        return geoPackage.getFeatureDao(getFeatureTableName(featureId));
    }


    /** returns the store id from an id with format storeId.featureTableName.idOfRow **/
    protected static String getStoreId(String featureId) {
        return featureId.split("\\.")[0];
    }

    /** returns the feature table name from an id with format storeId.featureTableName.idOfRow **/
    protected static String getFeatureTableName(String featureId) {
        if (featureId.split("\\.").length >= 1) {
            return featureId.split("\\.")[1];
        } else {
            return null;
        }
    }

    /** returns the rowId from an id with format storeId.featureTableName.idOfRow **/
    protected static String getRowId(String featureId) {
        if (featureId.split("\\.").length >= 2) {
            return featureId.split("\\.")[2];
        } else {
            // TODO: how should we handle new feature ids...just put a -1 or put nothing?
            return null;
        }
    }

    /**
     * Returns a byte array containing the Geometry as a GeoPackage WKB defined by <a
     * href="http://www.geopackage.org/spec/#gpb_data_blob_format"> the spec</a>.
     */
    private byte[] getStandardGeoPackageBinary(Geometry geometry) throws IOException {
        byte[] bytes;
        ByteWriter writer = new ByteWriter();

        // Write GP as the 2 byte magic number
        writer.writeString(GeoPackageConstants.GEO_PACKAGE_GEOMETRY_MAGIC_NUMBER);

        // Write a byte as the version, value of 0 = version 1
        writer.writeByte(GeoPackageConstants.GEO_PACKAGE_GEOMETRY_VERSION_1);

        // Build and write a flags byte
        byte flags = buildFlagsByte(geometry.isEmpty());
        writer.writeByte(flags);
        writer.setByteOrder(java.nio.ByteOrder.nativeOrder());

        // Write the 4 byte srs id int
        writer.writeInt(geometry.getSRID());

        // Write the envelope
        writer.writeDouble(geometry.getEnvelopeInternal().getMinX());
        writer.writeDouble(geometry.getEnvelopeInternal().getMaxX());
        writer.writeDouble(geometry.getEnvelopeInternal().getMinY());
        writer.writeDouble(geometry.getEnvelopeInternal().getMaxY());

        // Write the WKBGeometry
        WkbGeometryWriter.writeGeometry(writer,
                // create mil.nga.wkb.geom.Geometry from WKBGeometry byte array
                WkbGeometryReader.readGeometry(new ByteReader(new WKBWriter().write(geometry)))
        );

        // Get the bytes
        bytes = writer.getBytes();

        // Close the writer
        writer.close();

        return bytes;
    }

    /**
     * Build the flags byte from the flag values.  See http://www.geopackage.org/spec/#flags_layout
     * @return envelope indicator
     */
    private byte buildFlagsByte(boolean empty) {

        byte flag = 0;

        // Add the binary type to bit 5, 0 for standard and 1 for extended
        int binaryType =  0;
        flag += (binaryType << 5);

        // Add the empty geometry flag to bit 4, 0 for non-empty and 1 for
        // empty
        int emptyValue = empty ? 1 : 0;
        flag += (emptyValue << 4);

        // Add the envelope contents indicator code (3-bit unsigned integer to bits 3, 2, and 1)
        int envelopeIndicator = empty ? 0 : 1;
        flag += (envelopeIndicator << 1);

        // Add the byte order to bit 0, 0 for Big Endian and 1 for Little Endian
        int byteOrderValue = (java.nio.ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 0 : 1;
        flag += byteOrderValue;

        return flag;
    }
}
