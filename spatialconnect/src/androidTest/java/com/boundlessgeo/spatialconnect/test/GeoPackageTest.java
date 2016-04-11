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

import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.db.SCKVPStore;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreException;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;


public class GeoPackageTest extends BaseTestCase {

    private static SCServiceManager serviceManager;
    private final static String HAITI_GPKG_ID = "a5d93796-5026-46f7-a2ff-e5dec85heh6b";
    private final static String WHITEHORSE_GPKG_ID = "ba293796-5026-46f7-a2ff-e5dec85heh6b";

    @BeforeClass
    public static void setUp() throws Exception {
        testContext.deleteDatabase("Haiti");
        testContext.deleteDatabase("Whitehorse");
        testContext.deleteDatabase(SCKVPStore.DATABASE_NAME);
        serviceManager = new SCServiceManager(activity);
        serviceManager.addConfig(testConfigFile);
        serviceManager.startAllServices();
        waitForStoreToStart(HAITI_GPKG_ID);
        waitForStoreToStart(WHITEHORSE_GPKG_ID);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testContext.deleteDatabase("Haiti");
        testContext.deleteDatabase("Whitehorse");
        testContext.deleteDatabase(SCKVPStore.DATABASE_NAME);
    }

    @Test
    public void testThatDataServiceStartedGeoPackageStore() {
        boolean containsGeoPackageStore = false;
        for (SCDataStore store : serviceManager.getDataService().getActiveStores()) {
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
        SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(HAITI_GPKG_ID);
        assertNotNull("The store should downloaded locally", gpkgStore);
        assertEquals("The store should be running", SCDataStoreStatus.SC_DATA_STORE_RUNNING, gpkgStore.getStatus());
    }

    @Test
    public void testGeoPackageQueryWithin() {
        // this is the geopackage located http://www.geopackage.org/data/haiti-vectors-split.gpkg
        SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(HAITI_GPKG_ID);

        // bbox around part of haiti
        SCBoundingBox bbox = new SCBoundingBox(-73, 18, -72, 19);
        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
        );
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.query(filter).count().subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        assertEquals("The query should have the first 100 features",
                (Integer) 100,
                (Integer) testSubscriber.getOnNextEvents().get(0)
        );
        testSubscriber = new TestSubscriber();
        gpkgStore.query(filter).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
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
        SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(HAITI_GPKG_ID);

        // bbox around part of haiti
        SCBoundingBox bbox = new SCBoundingBox(-73, 18, -72, 19);
        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_CONTAINS)
        );
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.query(filter).count().subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        assertEquals("The query should have the first 100 features",
                (Integer) 100,
                (Integer) testSubscriber.getOnNextEvents().get(0)
        );
        testSubscriber = new TestSubscriber();
        gpkgStore.query(filter).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
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
    public void testGeoPackageCreateFeature() {
        SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(HAITI_GPKG_ID);
        SCSpatialFeature newFeature = getTestHaitiPoint();
        // remove the id b/c we want the db to create that for us
        newFeature.setStoreId("");
        gpkgStore.create(newFeature).subscribe(new Action1<SCSpatialFeature>() {
            @Override
            public void call(SCSpatialFeature created) {
                assertTrue("The new feature should have an id.", !created.getId().equals(""));
            }
        });
    }

    @Test
    public void testGeoPackageUpdateFeature() {
        final SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(HAITI_GPKG_ID);
        final SCSpatialFeature pointToUpdate = getTestHaitiPoint();
        gpkgStore.update(pointToUpdate).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean updated) {
                assertTrue("The feature should have been updated", updated);
                FeatureDao featureDao = ((GeoPackageStore) gpkgStore).getFeatureDao(
                        pointToUpdate.getKey().getLayerId()
                );
                FeatureRow featureRow = featureDao.queryForIdRow(Long.valueOf(pointToUpdate.getId()));
                assertEquals("The value of the 'featureid' column should have been updated.",
                        "featureid_value",
                        featureRow.getValue("featureid")
                );
            }
        });

    }

    @Test
    public void testGeoPackageDeleteFeature() {
        final SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(HAITI_GPKG_ID);
        final SCSpatialFeature pointToUpdate = getTestHaitiPoint();
        final Long featureId = Long.valueOf(pointToUpdate.getId());
        gpkgStore.delete(pointToUpdate.getKey()).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean deleted) {
                assertTrue("The featureRow should have been deleted", deleted);
                FeatureDao featureDao = ((GeoPackageStore) gpkgStore).getFeatureDao(
                        pointToUpdate.getKey().getLayerId()
                );
                assertTrue("The result should be null b/c the feature should have been deleted",
                        featureDao.queryForIdRow(featureId) == null
                );
            }
        });
    }

    @Test
    public void testGeoPackageCreateFeatureThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(WHITEHORSE_GPKG_ID);
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.create(getTestHaitiPoint()).subscribe(testSubscriber);
        testSubscriber.assertError(SCDataStoreException.class);
        assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
        );
    }

    @Test
    public void testGeoPackageUpdateFeatureThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(WHITEHORSE_GPKG_ID);
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.update(getTestHaitiPoint()).subscribe(testSubscriber);
        testSubscriber.assertError(SCDataStoreException.class);
        assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
        );

    }

    @Test
    public void testGeoPackageDeleteFeatureThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(WHITEHORSE_GPKG_ID);
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.delete(getTestHaitiPoint().getKey()).subscribe(testSubscriber);
        testSubscriber.assertError(SCDataStoreException.class);
        assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
        );
    }

    @Test
    public void testGeoPackageQueryByIdThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(WHITEHORSE_GPKG_ID);
        TestSubscriber testSubscriber = new TestSubscriber();
        gpkgStore.queryById(getTestHaitiPoint().getKey()).subscribe(testSubscriber);
        testSubscriber.assertError(SCDataStoreException.class);
        assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
        );
    }

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        serviceManager.getDataService().storeStarted(storeId).timeout(5, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }


    // helper method to return a sample SCSpatialFeature to use in CRUD to/from a GeoPackage.
    private SCSpatialFeature getTestHaitiPoint() {
        SCSpatialFeature scSpatialFeature = new SCSpatialFeature();
        // pick a random number between 1 an 100
        scSpatialFeature.setId(String.valueOf(new Random().nextInt((100))));
        scSpatialFeature.setLayerId("point_features");
        scSpatialFeature.setStoreId(HAITI_GPKG_ID);
        scSpatialFeature.getProperties().put("prop1", "prop1_value");
        scSpatialFeature.getProperties().put("featureid", "featureid_value");
        return scSpatialFeature;
    }
}
