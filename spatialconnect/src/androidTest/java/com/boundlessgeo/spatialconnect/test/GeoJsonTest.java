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
package com.boundlessgeo.spatialconnect.test;


import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.stores.GeoJsonStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCSpatialStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GeoJsonTest extends BaseTestCase {

    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.getConfigService().addConfigFilePath(
                String.format("%s/s%",
                        localConfigFile.getAbsolutePath(),
                        localConfigFile.getName()));
        sc.startAllServices();
        waitForStoreToStart(BARS_GEO_JSON_ID);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        testContext.deleteFile(BARS_GEO_JSON_ID + ".json");
    }

    @Test
    public void testThatDataServiceStartedGeoJsonStore() {
        boolean containsGeoJsonStore = false;
        for (SCDataStore store : sc.getDataService().getActiveStores()) {
            assertTrue("The store should be running.",
                    store.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)
            );
            if (store.getType().equals("geojson")) {
                if (store.getStoreId().equals(BARS_GEO_JSON_ID)) {
                    containsGeoJsonStore = true;
                }
            }
        }
        assertTrue("A geojson store should be running.", containsGeoJsonStore);
    }

    @Test
    public void testSearchGeoJsonStore() {
        SCDataStore geoJsonStore = sc.getDataService().getStoreById(BARS_GEO_JSON_ID);
        SCBoundingBox bbox = new SCBoundingBox(-77.11974,38.803149,-76.909393,38.995548);
        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
        );
        TestSubscriber testSubscriber = new TestSubscriber();
        ((SCSpatialStore) geoJsonStore).query(filter).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        assertEquals("The query should have returned 16 features.",
                (Integer) 30,
                (Integer) testSubscriber.getOnNextEvents().size()
        );
        SCSpatialFeature feature = (SCSpatialFeature) testSubscriber.getOnNextEvents().get(0);
        assertEquals("The store id should be for the test geojson store.",
                BARS_GEO_JSON_ID,
                feature.getKey().getStoreId()
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
    public void testStoreDestroy() {
        GeoJsonStore store = (GeoJsonStore) sc.getDataService().getStoreById(BARS_GEO_JSON_ID);
        final File geoJsonFile = new File(store.getPath());
        assertTrue("GeoJson file should exist", geoJsonFile.exists());

        store.destroy();
        assertEquals(geoJsonFile.exists(), false);

    }

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(5, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

}

