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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import rx.schedulers.Schedulers;

/**
 * Provides capabilities for interacting with a single GeoPackage.
 */
public class GeoPackageStore extends SCDataStore {

    private static final String STORE_NAME = "GeoPackageStore";
    private static final String LOG_TAG = GeoPackageStore.class.getSimpleName();
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    private static String TYPE = "gpkg";
    private static int VERSION = 1;

    public static String versionKey() {
        return TYPE + "." + VERSION;
    }

    /**
     * Constructor for GeoPackageStore that initializes the data store adapter
     * based on the scStoreConfig.
     *
     * @param context       instance of the current activity's context
     * @param scStoreConfig instance of the configuration needed to configure the store
     */
    public GeoPackageStore(Context context, SCStoreConfig scStoreConfig) {
        super(context, scStoreConfig);
        // TODO: revist if we want to have setters
        this.setName(scStoreConfig.getName());
        this.setType(TYPE);
        this.setVersion(VERSION);
        this.getKey();
        // setup the adapter for this store
        GeoPackageAdapter adapter = new GeoPackageAdapter(context, scStoreConfig, this);
        this.setAdapter(adapter);
    }

    @Override
    public DataStorePermissionEnum getAuthorization() {
        return DataStorePermissionEnum.READ_WRITE;
    }

    @Override
    public Observable<SCSpatialFeature> query(final SCQueryFilter scFilter) {
        return Observable.create(new Observable.OnSubscribe<SCSpatialFeature>() {
            @Override
            public void call(Subscriber<? super SCSpatialFeature> subscriber) {
                final GeoPackageAdapter adapter =
                  (GeoPackageAdapter) GeoPackageStore.this.getAdapter();
                final GeoPackageManager manager = adapter.getGeoPackageManager();
                String geopackageName = adapter.getDataStoreName();
                final GeoPackage gp = manager.open(geopackageName);
                List<String> layerIds = scFilter.getLayerIds();
                List<String> featureTables = gp.getFeatureTables();
                List<String> queryTables = new ArrayList<>();

                if (featureTables.size() < 1) {
                  subscriber.onCompleted();
                }

                if (layerIds.size() != 0) {
                    for (String tName : layerIds) {
                        for (String fName: featureTables) {
                            if (tName.equalsIgnoreCase(fName)) {
                                queryTables.add(fName);
                            }
                        }
                    }
                } else {
                    queryTables.addAll(featureTables);
                }

                int limit = scFilter.getLimit();
                int featuresCount = 0;
                int numLayers = queryTables.size();
                int perLayer = limit / numLayers;

                for (String featureTableName : queryTables){
                    FeatureDao featureDao = gp.getFeatureDao(featureTableName);
                    FeatureCursor featureCursor = null;
                    try {
                        featureCursor = featureDao.queryForAll();
                        int layerCount = 0;
                        while (featureCursor != null && !featureCursor.isClosed()
                                && featureCursor.moveToNext() && featuresCount < limit
                                && layerCount <= perLayer) {
                            try {
                                SCSpatialFeature feature = createSCSpatialFeature(featureCursor.getRow());
                                if (feature instanceof SCGeometry &&
                                        ((SCGeometry) feature).getGeometry() != null &&
                                        scFilter.getPredicate().isInBoundingBox((SCGeometry) feature)) {
                                    featuresCount++;
                                    layerCount++;
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
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Couldn't send next feature");
                    } finally {
                      if (featureCursor != null) {
                        featureCursor.close();
                      }
                    }
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
          .onBackpressureBuffer();  // this is needed otherwise the filter can't keep up and will
        // throw MissingBackpressureException
    }

    @Override
    public Observable<SCSpatialFeature> queryById(final SCKeyTuple keyTuple) {
        return Observable.create(new Observable.OnSubscribe<SCSpatialFeature>(){

            @Override
            public void call(Subscriber<? super SCSpatialFeature> subscriber) {
                final GeoPackageAdapter adapter =
                    (GeoPackageAdapter) GeoPackageStore.this.getAdapter();
                final GeoPackageManager manager = adapter.getGeoPackageManager();
                String geopackageName = adapter.getDataStoreName();
                final GeoPackage gp = manager.open(geopackageName);
                FeatureDao featureDao = gp.getFeatureDao(keyTuple.getLayerId());
                FeatureCursor featureCursor = featureDao.queryForId(Long.parseLong(keyTuple.getFeatureId()));
                if (featureCursor.moveToFirst()) {
                    try {
                        SCSpatialFeature feature = createSCSpatialFeature(featureCursor.getRow());
                        subscriber.onNext(feature);
                    } catch (ParseException e) {
                        Log.w(LOG_TAG, "Couldn't parse the geometry.");
                        subscriber.onError(e);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Unexpected exception.");
                        subscriber.onError(e);
                    } finally {
                      featureCursor.close();
                      subscriber.onCompleted();
                    }

                }
            }
        });

    }

    @Override
    public Observable<SCSpatialFeature> create(final SCSpatialFeature scSpatialFeature) {
        final String layerId = scSpatialFeature.getKey().getLayerId();
        final FeatureDao featureDao = getFeatureDao(layerId);  // layerId is the table name
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

        return Observable.create(new Observable.OnSubscribe<SCSpatialFeature>() {

            @Override
            public void call(Subscriber<? super SCSpatialFeature> subscriber) {
                try {
                    long rowId = featureDao.create(newRow);
                    FeatureRow newRow = featureDao.queryForIdRow(rowId);
                    subscriber.onNext(createSCSpatialFeature(newRow));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Could not create feature");
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
        final String layerId = scSpatialFeature.getKey().getLayerId();
        final FeatureDao featureDao = getFeatureDao(layerId);  // layerId is the table name

        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    if (rowToUpdate != null && featureDao.update(rowToUpdate) == 1) {
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
        final String layerId = tuple.getLayerId();
        final FeatureDao featureDao = getFeatureDao(layerId);  // layerId is the table name

        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    Long rowId = Long.valueOf(featureId);
                    if (featureDao.deleteById(rowId) == 1) {
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

      storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_STARTED);

        return Observable.create(new Observable.OnSubscribe<SCStoreStatusEvent>() {
            @Override
            public void call(final Subscriber<? super SCStoreStatusEvent> subscriber) {

                // subscribe to an Observable/stream that lets us know when the adapter is connected or disconnected
                storeInstance.getAdapter().connect().subscribe(new Subscriber<SCDataAdapterStatus>() {

                    @Override
                    public void onCompleted() {
                        storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w(LOG_TAG, "Could not activate data store " + storeId);
                        storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
                        subscriber.onError(new Exception("Could not activate data store " + storeId));
                    }

                    @Override
                    public void onNext(SCDataAdapterStatus status) {
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
     * @param row a populated FeatureRow instance
     * @return SCSpatialFeature instance populated with the data from the FeatureRow
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
        // populate properties map with data from each column
        Map<String, Object> props = scSpatialFeature.getProperties();
        for (String columnName : row.getColumnNames()) {
            if (null != row.getValue(columnName) && !row.getColumn(columnName).isGeometry()) {
                props.put(columnName, row.getValue(columnName));
            }
        }
        // populate the store, layer and feature ids
        scSpatialFeature.setStoreId(this.getStoreId());
        scSpatialFeature.setLayerId(row.getTable().getTableName()); // layer is a table name in a geopackage
        scSpatialFeature.setId(String.valueOf(row.getId())); // id is the id of the row in that table

        return scSpatialFeature;
    }

    /**
     * Helper method that returns a new FeatureRow instance based on the properties of an
     * SCSptaialFeature instance.  If the SCSpatialFeature instance represents an existing
     * FeatureRow, then that row is used, otherwise a new instance is created.
     * <p/>
     * Note that it is not possible to set the id of a new FeatureRow because the
     * <a href="http://www.geopackage.org/spec/#requirement_feature_integer_pk">
     * GeoPackage specification</a> requires that the id is auto incremented.  Also, when
     * constructing the FeatureRow, this method will ignore any properties of the SCSpatialFeature
     * instance that do not already have a corresponding column in the feature table.
     *
     * @param scSpatialFeature instance of an SCSpatialFeature that will be converted to a new
     *                         FeatureRow instance.
     * @return a FeatureRow instance populated with all the properties from the scSpatialFeature
     */
    protected FeatureRow toFeatureRow(final SCSpatialFeature scSpatialFeature) {
        final String featureId = scSpatialFeature.getKey().getFeatureId();
        final String layerId = scSpatialFeature.getKey().getLayerId();
        final FeatureDao featureDao = getFeatureDao(layerId);  // layerId is the table name

        FeatureRow featureRow = featureDao.queryForIdRow(Long.valueOf(featureId));
        if (featureRow == null) {
            featureRow = featureDao.newRow();
        }

        // add all the properties to the matching columns
        for (String key : scSpatialFeature.getProperties().keySet()) {
            if (Arrays.asList(featureDao.getTable().getColumnNames()).contains(key)) {
                if (!key.equals(featureRow.getPkColumn().getName())) {
                    // ugly casting to make types pass validation
                    String stringValue = String.valueOf(scSpatialFeature.getProperties().get(key));
                    switch (featureRow.getColumn(key).getDataType()) {
                        case SMALLINT: {
                            featureRow.setValue(key, Short.valueOf(stringValue));
                            break;
                        }
                        case MEDIUMINT: {
                            featureRow.setValue(key, Integer.valueOf(stringValue));
                            break;
                        }
                        case INT: {
                            featureRow.setValue(key, Long.valueOf(stringValue));
                            break;
                        }
                        case INTEGER: {
                            featureRow.setValue(key, Long.valueOf(stringValue));
                            break;
                        }
                        case DOUBLE: {
                            featureRow.setValue(key, Double.valueOf(stringValue));
                            break;
                        }
                        case REAL: {
                            featureRow.setValue(key, Double.valueOf(stringValue));
                            break;
                        }
                        default: {
                            featureRow.setValue(key,
                                    featureRow.getColumn(key).getDataType().getClassType().cast(stringValue)
                            );
                            break;
                        }
                    }
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
     * Helper method that returns the FeatureDao instance based on the feature table name.
     *
     * @param featureTableName the name of the SCSpatialFeature's layer attribute
     * @return the instance of the FeatureDao used to interact with the feature table
     */
    public FeatureDao getFeatureDao(String featureTableName) {
        GeoPackageManager manager = ((GeoPackageAdapter) this.getAdapter()).getGeoPackageManager();
        GeoPackage geoPackage = manager.open(this.getAdapter().getDataStoreName());
        return geoPackage.getFeatureDao(featureTableName);
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
     *
     * @return envelope indicator
     */
    private byte buildFlagsByte(boolean empty) {

        byte flag = 0;

        // Add the binary type to bit 5, 0 for standard and 1 for extended
        int binaryType = 0;
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
