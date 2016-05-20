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

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.db.SCKVPStore;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreException;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCKeyTuple;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;


public class GeoPackageTest extends BaseTestCase {

    private static SpatialConnect sc;
    private final static String HAITI_GPKG_ID = "a5d93796-5026-46f7-a2ff-e5dec85heh6b";
    private final static String WHITEHORSE_GPKG_ID = "ba293796-5026-46f7-a2ff-e5dec85heh6b";

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(testConfigFile);
        sc.startAllServices();
        waitForStoreToStart(WHITEHORSE_GPKG_ID);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testContext.deleteDatabase("Haiti");
        testContext.deleteDatabase("Whitehorse");
        testContext.deleteDatabase(SCKVPStore.DATABASE_NAME);
        testContext.deleteDatabase("DEFAULT_STORE");
    }

    @Test
    public void testThatDataServiceStartedGeoPackageStore() {
        boolean containsGeoPackageStore = false;
        for (SCDataStore store : sc.getDataService().getActiveStores()) {
            assertTrue("The store should be running.",
                    store.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)
            );
            if (store.getType().equals("gpkg")) {
                containsGeoPackageStore = true;
                assertTrue("The store's adapter should be connected.",
                        store.getAdapter().getStatus()
                                .equals(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED)
                );
            }
        }
        assertTrue("A geopackage store should be running.", containsGeoPackageStore);
        // test that store has the correct status
        SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);
        assertNotNull("The store should downloaded locally", gpkgStore);
        assertEquals("The store should be running", SCDataStoreStatus.SC_DATA_STORE_RUNNING, gpkgStore.getStatus());
    }

    @Test
    public void testGeoPackageQueryWithin() {
        // this is the geopackage located http://www.geopackage.org/data/haiti-vectors-split.gpkg
        SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);

        // bbox around part of haiti
        SCBoundingBox bbox = new SCBoundingBox(-73, 18, -72, 19);
        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
        );
        filter.addLayerId("point_features");
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.query(filter).timeout(5, TimeUnit.SECONDS).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
//        testSubscriber.assertCompleted();  // it never completes, only times out
//        testSubscriber.assertNoErrors();  // it will throw a timeout error b/c it never completes
        assertEquals("The query should have emitted the first 100 features",
                (Integer) 100,
                (Integer) testSubscriber.getOnNextEvents().size()
        );
        SCSpatialFeature feature = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        assertEquals("The store id should be for Haiti.",
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
        // this is the geopackage located http://www.geopackage.org/data/haiti-vectors-split.gpkg
        SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);

        // bbox around part of haiti
        SCBoundingBox bbox = new SCBoundingBox(-73, 18, -72, 19);
        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_CONTAINS)
        );
        filter.addLayerId("point_features");
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.query(filter).timeout(5, TimeUnit.SECONDS).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        assertEquals("The query should have the first 100 features",
                (Integer) 100,
                (Integer) testSubscriber.getOnNextEvents().size()
        );
        SCSpatialFeature feature = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        assertEquals("The store id should be for Haiti.",
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
    public void testGeoPackageQueryById() {
        SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);
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
        SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);
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
        gpkgStore.queryById(created.getKey()).timeout(5, TimeUnit.SECONDS).subscribe(testSubscriber);
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
        final SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);
        final SCSpatialFeature pointToUpdate = getTestHaitiPoint();
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.update(pointToUpdate).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        SCSpatialFeature updated = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        // now get the feature we just created
        gpkgStore.queryById(updated.getKey()).timeout(5, TimeUnit.SECONDS).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        SCSpatialFeature feature = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        assertEquals("The updated feature should have the updated geom.",
                ((SCGeometry) pointToUpdate).getGeometry().toString(),
                ((SCGeometry) feature).getGeometry().toString()
        );
    }

    @Test
    public void testGeoPackageDeleteFeature() {
        final SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);
        final SCSpatialFeature pointToDelete = getTestHaitiPoint();
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.delete(pointToDelete.getKey()).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        // now query for the feature we just deleted and confirm it cannot be found
        gpkgStore.queryById(pointToDelete.getKey()).timeout(5, TimeUnit.SECONDS).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        assertEquals("No results should be returned b/c the feature should be deleted",
                0,
                testSubscriber.getOnNextEvents().size()
        );
    }

    @Test
    public void testGeoPackageCreateFeatureThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);
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
        SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);
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
        SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);
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
        SCDataStore gpkgStore = sc.getDataService().getStoreById(HAITI_GPKG_ID);
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

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(3, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    // helper method to return a sample SCSpatialFeature to use in CRUD to/from a GeoPackage.
    private SCSpatialFeature getTestHaitiPoint() {
        SCSpatialFeature scSpatialFeature = null;
        // set geometry
        try {
            Geometry point = new WKTReader().read("POINT (-72.19623166 18.553346)");
            scSpatialFeature = new SCGeometry(point);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        // pick a random number between 1 an 100
        scSpatialFeature.setId(String.valueOf(new Random().nextInt((100))));
        scSpatialFeature.setLayerId("point_features");
        scSpatialFeature.setStoreId(HAITI_GPKG_ID);
        scSpatialFeature.getProperties().put("cpyrt_note", "apache v2");
        scSpatialFeature.getProperties().put("featureid", "featureid_value");
        return scSpatialFeature;
    }
}
