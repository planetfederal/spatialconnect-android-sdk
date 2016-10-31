/**
 * Copyright 2015-2016 Boundless, http://boundlessgeo.com
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
package com.boundlessgeo.spatialconnect.test;

import android.database.Cursor;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;
import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.db.SCSqliteHelper;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreException;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCKeyTuple;
import com.boundlessgeo.spatialconnect.stores.SCSpatialStore;
import com.boundlessgeo.spatialconnect.stores.SCStoreStatusEvent;
import com.squareup.sqlbrite.BriteDatabase;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class GeoPackageTest extends BaseTestCase {

    private static SpatialConnect sc;
    private static int TIMEOUT = 2;
    private static SCBoundingBox HAITI_BOUNDING_BOX = new SCBoundingBox(-73.5315, 18.0728, -71.0211, 19.994);
    private static BriteDatabase haiti;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(localConfigFile);
        sc.startAllServices();
        haiti = new SCSqliteHelper(testContext, HAITI_GPKG_ID).db();
        waitForStoreToStart(HAITI_GPKG_ID);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        testContext.deleteDatabase(HAITI_GPKG_ID);
    }

    @Test
    public void testThatDataServiceStartedGeoPackageStore() {
        SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);

        assertTrue("The store's adapter should be connected.",
                gpkgStore.getAdapter().getStatus()
                        .equals(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED));
        assertNotNull("The store should downloaded locally", gpkgStore);
        assertEquals("The store should be running", SCDataStoreStatus.SC_DATA_STORE_RUNNING, gpkgStore.getStatus());
    }

    @Test
    public void testGeoPackageQueryStore() {
        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(HAITI_BOUNDING_BOX, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
        );
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().queryStores(Arrays.asList(HAITI_GPKG_ID), filter)
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        assertTrue("The query should have returned some features.", testSubscriber.getOnNextEvents().size() > 0);
    }

    @Test
    public void testGeoPackageQueryWithin() {
        SCSpatialStore gpkgStore = ((SCSpatialStore) sc.getDataService().getStoreById(HAITI_GPKG_ID));

        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(HAITI_BOUNDING_BOX, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
        );
        filter.addLayerId("point_features");
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.query(filter).timeout(2, TimeUnit.SECONDS).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        //testSubscriber.assertCompleted();
        //testSubscriber.assertNoErrors();
        assertEquals("The query should have the first 100 features, b/c we specify a layer without a limit.",
                (Integer) 100,
                (Integer) testSubscriber.getOnNextEvents().size()
        );
        SCSpatialFeature feature = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        assertEquals("The store id should be for Rio.",
                HAITI_GPKG_ID,
                feature.getKey().getStoreId()
        );
        assertEquals("The layer should be point_features.",
                "point_features",
                feature.getKey().getLayerId()
        );
        assertEquals("The id should match the featureId from the key tuple.",
                feature.getId(),
                feature.getKey().getFeatureId()
        );
        assertTrue("The feature should have properties.",
                feature.getProperties().size() > 0
        );
    }

    @Test
    public void testGeoPackageQueryContains() {
        SCSpatialStore gpkgStore = ((SCSpatialStore) sc.getDataService().getStoreById(HAITI_GPKG_ID));

        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(HAITI_BOUNDING_BOX, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
        );
        filter.addLayerId("point_features");
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.query(filter).timeout(10, TimeUnit.SECONDS).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        assertEquals("The query should have the first 100 features, b/c we specify a layer without a limit.",
                (Integer) 100,
                (Integer) testSubscriber.getOnNextEvents().size()
        );
        SCSpatialFeature feature = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        assertEquals("The store id should be for Rio.",
                HAITI_GPKG_ID,
                feature.getKey().getStoreId()
        );
        assertEquals("The layer should be police_stations.",
                "point_features",
                feature.getKey().getLayerId()
        );
        assertEquals("The id should match the featureId from the key tuple.",
                feature.getId(),
                feature.getKey().getFeatureId()
        );
        assertTrue("The feature should have properties.",
                feature.getProperties().size() > 0
        );
    }

    @Test
    public void testGeoPackageQueryById() {
        SCSpatialStore gpkgStore = ((SCSpatialStore) sc.getDataService().getStoreById(HAITI_GPKG_ID));
        SCKeyTuple featureKey = new SCKeyTuple(HAITI_GPKG_ID, "point_features", "1");
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.queryById(featureKey).timeout(10, TimeUnit.SECONDS).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        assertEquals("The query should have returned 1 feature",
                (Integer) 1,
                (Integer) testSubscriber.getOnNextEvents().size()
        );
        SCSpatialFeature feature = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        assertEquals("The id of the returned feature should be 1.",
                "1",
                feature.getKey().getFeatureId()
        );
    }

    @Test
    public void testGeoPackageCreateFeature() {
        SCSpatialStore gpkgStore = ((SCSpatialStore) sc.getDataService().getStoreById(HAITI_GPKG_ID));
        SCSpatialFeature newFeature = getTestHaitiPoint();
        // remove the feature id b/c we want the db to create that for us
        newFeature.setId("");
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.create(newFeature).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        SCSpatialFeature created = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        assertTrue("The new feature should have an id.", !created.getId().equals(""));
        testSubscriber = new TestSubscriber();
        gpkgStore.queryById(created.getKey()).timeout(TIMEOUT, TimeUnit.SECONDS).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        SCSpatialFeature feature = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        assertTrue("The new feature should have an id.", !feature.getId().equals(""));
        assertEquals("The new feature should have a geometry.",
                ((SCGeometry) newFeature).getGeometry().toString(),
                ((SCGeometry) feature).getGeometry().toString()
        );
    }

    @Test
    public void testGeoPackageUpdateFeature() {
        final SCSpatialStore gpkgStore = ((SCSpatialStore) sc.getDataService().getStoreById(HAITI_GPKG_ID));
        final SCSpatialFeature pointToUpdate = getTestHaitiPoint();
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.update(pointToUpdate).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        SCSpatialFeature updated = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        // now get the feature we just created
        gpkgStore.queryById(updated.getKey()).timeout(TIMEOUT, TimeUnit.SECONDS).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        SCSpatialFeature feature = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        assertEquals("The updated feature should have the updated geom.",
                ((SCGeometry) pointToUpdate).getGeometry().toString(),
                ((SCGeometry) feature).getGeometry().toString()
        );
    }

    @Test
    public void testGeoPackageDeleteFeature() {
        final SCSpatialStore gpkgStore = ((SCSpatialStore) sc.getDataService().getStoreById(HAITI_GPKG_ID));
        final SCSpatialFeature pointToDelete = getTestLinearFeatureHaitiPoint();
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.delete(pointToDelete.getKey()).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        // now query for the feature we just deleted and confirm it cannot be found
        gpkgStore.queryById(pointToDelete.getKey()).timeout(TIMEOUT, TimeUnit.SECONDS).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        assertEquals("No results should be returned b/c the feature should be deleted",
                0,
                testSubscriber.getOnNextEvents().size()
        );
    }

    @Test
    public void testGeoPackageCreateFeatureThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        SCSpatialStore gpkgStore = ((SCSpatialStore) sc.getDataService().getStoreById(HAITI_GPKG_ID));
        TestSubscriber testSubscriber = new TestSubscriber();
        SCSpatialFeature feature = getTestHaitiPoint();
        feature.setLayerId("invalid_table_name");
        gpkgStore.create(feature).subscribe(testSubscriber);
        testSubscriber.assertError(SCDataStoreException.class);
        assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
        );
    }

    @Test
    public void testGeoPackageUpdateFeatureThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        SCSpatialStore gpkgStore = ((SCSpatialStore) sc.getDataService().getStoreById(HAITI_GPKG_ID));
        TestSubscriber testSubscriber = new TestSubscriber();
        SCSpatialFeature feature = getTestHaitiPoint();
        feature.setLayerId("invalid_table_name");
        gpkgStore.update(feature).subscribe(testSubscriber);
        testSubscriber.assertError(SCDataStoreException.class);
        assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
        );

    }

    @Test
    public void testGeoPackageDeleteFeatureThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        SCSpatialStore gpkgStore = ((SCSpatialStore) sc.getDataService().getStoreById(HAITI_GPKG_ID));
        TestSubscriber testSubscriber = new TestSubscriber();
        SCSpatialFeature feature = getTestHaitiPoint();
        feature.setLayerId("invalid_table_name");
        gpkgStore.delete(feature.getKey()).subscribe(testSubscriber);
        testSubscriber.assertError(SCDataStoreException.class);
        assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
        );
    }

    @Test
    public void testGeoPackageQueryByIdThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        SCSpatialStore gpkgStore = ((SCSpatialStore) sc.getDataService().getStoreById(HAITI_GPKG_ID));
        TestSubscriber testSubscriber = new TestSubscriber();
        SCSpatialFeature feature = getTestHaitiPoint();
        feature.setLayerId("invalid_table_name");
        gpkgStore.queryById(feature.getKey()).subscribe(testSubscriber);
        testSubscriber.assertError(SCDataStoreException.class);
        assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
        );
    }

    @Ignore
    public void testInvalidGeoPackageIsNotStarted() {
        final TestSubscriber testSubscriber = new TestSubscriber();
        Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                sc.getDataService().storeEvents.autoConnect()
                        .timeout(TIMEOUT, TimeUnit.MINUTES)
                        .subscribe(new Action1<SCStoreStatusEvent>() {
                            @Override
                            public void call(SCStoreStatusEvent event) {
                                if (event.getStoreId().equals(HAITI_GPKG_ID) &&
                                        event.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_STOPPED)) {
                                    subscriber.onCompleted();
                                }
                            }
                        });
            }}).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        GeoPackageStore gpkgStore = (GeoPackageStore) sc.getDataService().getStoreById(HAITI_GPKG_ID);
        assertTrue("The geopackage should not be running b/c it is not valid.  The status was " +
                gpkgStore.getStatus().name(),
                gpkgStore.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_STOPPED)
        );
    }

    //test Feature sources

    @Test
    public void testGetGeoPackageContents() {
        SCDataStore whitehorse = sc.getDataService().getStoreById(HAITI_GPKG_ID);
        int contentsSize = ((GeoPackageAdapter) whitehorse.getAdapter()).getGeoPackageContents().size();
        assertEquals("The Haiti gpkg should only have 3 row in the gpkg_contents table.", 3, contentsSize);
    }

    @Test
    public void testGetFeatureSources() {
        SCDataStore store = sc.getDataService().getStoreById(HAITI_GPKG_ID);
        int featureSourcesSize = ((GeoPackageAdapter) store.getAdapter()).getFeatureSources().size();
        assertEquals("The Haiti gpkg should have 3 feature tables.", 3, featureSourcesSize);
    }

    //base sqliteHelperTest
    @Ignore
    public void testGeoPackageStoreCanBeAccessedBySCSqliteHelper() {
        Cursor cursor = haiti.query("SELECT COUNT(*) FROM point_features;");
        cursor.moveToFirst();
        assertEquals("The point_features table should only have 100 rows.", 100, cursor.getInt(0));
    }

    // test table creation functions
    @Ignore
    public void test_CreateSpatialIndex_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT CreateSpatialIndex(?, ?, ?);", new String[]{"point_features", "geom", "fid"});
        cursor.moveToFirst();
        Cursor cursor2 = haiti.query("SELECT COUNT(*) FROM rtree_police_stations_geom;");
        cursor2.moveToFirst();
        assertEquals("The spatial index tables should exist and be populated.", 150, cursor2.getInt(0));
    }

    // test geometry i/o functions
    @Test
    public void test_ST_AsText_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_AsText(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("The geom should start with Point if ST_AsText worked.", cursor.getString(0).startsWith("Point"));
    }

    @Ignore
    public void test_ST_GeomFromText_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_GeomFromText('Point (-72.981321 18.42740796)');");
        cursor.moveToFirst();
        assertTrue("ST_GeomFromText should return a blob if it loaded correctly.", cursor.getColumnCount() == 1);
        assertTrue("ST_GeomFromText should return a blob if it loaded correctly.", cursor.getBlob(0).length > 0);

    }

    @Test
    public void test_ST_WKTToSQL_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_WKTToSQL('Point (0 0)');");
        cursor.moveToFirst();
        assertTrue("ST_WKTToSQL should return a blob if it loaded correctly.", cursor.getColumnCount() == 1);
        assertTrue("ST_WKTToSQL should return a blob if it loaded correctly.", cursor.getBlob(0).length > 0);
    }


    // test geometry inspection functions
    @Test
    public void test_ST_MinX_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_MinX(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MinX should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_MaxX_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_MaxX(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MaxX should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_MinY_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_MinY(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MinY should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_MaxY_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_MaxY(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MaxY should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_SRID_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_SRID(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("ST_SRID should return 4326 geom column in point_features.", 4326, cursor.getInt(0));
    }

    @Test
    public void test_ST_IsMeasured_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_IsMeasured(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("ST_IsMeasured should return 0 for point features.", 0, cursor.getInt(0));
    }

    @Test
    public void test_ST_Is3d_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_Is3d(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("ST_Is3d should return 0 for point features.", 0, cursor.getInt(0));
    }

    @Test
    public void test_ST_CoordDim_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_CoordDim(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("The dimensions should be 2.", 2, cursor.getInt(0));
    }

    @Test
    public void test_ST_GeometryType_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_GeometryType(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("The geometry type should be Point.", "Point", cursor.getString(0));
    }


    private static void waitForStoreToStart(final String storeId) {
        System.out.println("Waiting for store " + storeId + " to start");
        TestSubscriber testSubscriber = new TestSubscriber();
        Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                SCDataService dataService = sc.getDataService();
                SCDataStore store = dataService.getStoreById(storeId);
                if (store != null) {
                    if (dataService.getStoreById(storeId).getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)) {
                        subscriber.onCompleted();
                    } else {
                        dataService.storeEvents.autoConnect()
                                .timeout(TIMEOUT, TimeUnit.MINUTES)
                                .subscribe(new Action1<SCStoreStatusEvent>() {
                                    @Override
                                    public void call(SCStoreStatusEvent event) {
                                        Log.e("GeoPackaeTEst", event.getStatus().toString());
                                        Log.e("GeoPackaeTEst", "eventStoreId " + event.getStoreId());
                                        Log.e("GeoPackaeTEst", "storeId " + storeId);
                                        if (event.getStoreId().equals(storeId) &&
                                                event.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)) {
                                            subscriber.onCompleted();
                                        }
                                    }
                                });
                    }
                }
            }
        }).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
    }

    // helper method to return a sample SCSpatialFeature to use in CRUD to/from a GeoPackage.
    private SCSpatialFeature getTestHaitiPoint() {
        SCSpatialFeature scSpatialFeature = null;
        // set geometry
        try {
            Geometry point = new WKTReader().read("POINT (-43.2173407165 -22.9281988216)");
            scSpatialFeature = new SCGeometry(point);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        // pick a random number between 1 an 100
        scSpatialFeature.setId(String.valueOf(new Random().nextInt((100))));
        scSpatialFeature.setLayerId("point_features");
        scSpatialFeature.setStoreId(HAITI_GPKG_ID);
        scSpatialFeature.getProperties().put("NAME", "some point feature 123");
        scSpatialFeature.getProperties().put("OTHER_TAGS", "we have dogs at this one");
        return scSpatialFeature;
    }

    private SCSpatialFeature getTestLinearFeatureHaitiPoint() {
        SCSpatialFeature scSpatialFeature = null;
        // set geometry
        try {
            Geometry point = new WKTReader().read("POINT (-43.2173407165 -22.9281988216)");
            scSpatialFeature = new SCGeometry(point);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        // pick a random number between 1 an 100
        scSpatialFeature.setId(String.valueOf(new Random().nextInt((100))));
        scSpatialFeature.setLayerId("linear_features");
        scSpatialFeature.setStoreId(HAITI_GPKG_ID);
        scSpatialFeature.getProperties().put("NAME", "some point feature 123");
        scSpatialFeature.getProperties().put("OTHER_TAGS", "we have dogs at this one");
        return scSpatialFeature;
    }
}
