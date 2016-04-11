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


import com.boundlessgeo.spatialconnect.dataAdapter.GeoJsonAdapter;
import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.db.SCKVPStore;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


public class GeoJsonTest extends BaseTestCase {

    private static SCServiceManager serviceManager;
    private final static String GEOJSON_STORE_ID = "50402599-3ad3-439f-9c49-3c8a7579933b";


    @BeforeClass
    public static void setUp() throws Exception {
        serviceManager = new SCServiceManager(testContext);
        serviceManager.addConfig(testConfigFile);
        serviceManager.startAllServices();
        waitForStoreToStart(GEOJSON_STORE_ID);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testContext.deleteFile("nola_polling_places.json");
        testContext.deleteDatabase("Haiti");
        testContext.deleteDatabase("Whitehorse");
        testContext.deleteDatabase(SCKVPStore.DATABASE_NAME);
    }

    @Test
    public void testThatDataServiceStartedGeoJsonStore() {
        boolean containsGeoJsonStore = false;
        for (SCDataStore store : serviceManager.getDataService().getActiveStores()) {
            assertTrue("The store should be running.",
                    store.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)
            );
            if (store.getType().equals("geojson")) {
                containsGeoJsonStore = true;
                assertTrue("The store's adapter should be connected",
                        store.getAdapter().getStatus().equals(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED)
                );
            }
        }
        assertTrue("A geojson store should be running.", containsGeoJsonStore);
    }

    @Test
    public void testSearchGeoJsonStore() {
        SCDataStore geoJsonStore = serviceManager.getDataService().getStoreById(GEOJSON_STORE_ID);
        SCBoundingBox bbox = new SCBoundingBox(-90.114326, 29.921762, -90.063558, 29.938573);
        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
        );
        TestSubscriber testSubscriber = new TestSubscriber();
        geoJsonStore.query(filter).count().subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        assertEquals("The query should have returned 16 features.",
                (Integer) 16,
                (Integer) testSubscriber.getOnNextEvents().get(0)
        );
        testSubscriber = new TestSubscriber();
        geoJsonStore.query(filter).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        SCSpatialFeature feature = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        assertEquals("The store id should be for the test geojson store.",
                GEOJSON_STORE_ID,
                feature.getKey().getStoreId()
        );
        assertEquals("The layer should be " + GeoJsonAdapter.DEFAULTLAYER,
                GeoJsonAdapter.DEFAULTLAYER,
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

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        serviceManager.getDataService().storeStarted(storeId).timeout(5, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

}

