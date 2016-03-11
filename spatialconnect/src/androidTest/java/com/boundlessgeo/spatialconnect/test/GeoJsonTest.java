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
package com.boundlessgeo.spatialconnect.test;


import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCStoreStatusEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.functions.Action1;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


public class GeoJsonTest extends BaseTestCase {

    private SCServiceManager serviceManager;
    private final static String GEOJSON_STORE_ID = "50402599-3ad3-439f-9c49-3c8a7579933b";


    @Before
    public void setUp() throws Exception {
        serviceManager = new SCServiceManager(activity, testConfigFile);
    }

    @After
    public void tearDown() throws Exception {
        testContext.deleteFile("nola_polling_places.json");
    }

    @Test
    public void testThatDataServiceStartedGeoJsonStore() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
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
        });
    }

    @Test
    public void testSearchGeoJsonStore() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
                SCDataStore geoJsonStore = serviceManager.getDataService().getStoreById(GEOJSON_STORE_ID);
                SCBoundingBox bbox = new SCBoundingBox(-90.114326, 29.921762, -90.063558, 29.938573);
                SCQueryFilter filter = new SCQueryFilter(
                        new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
                );
                geoJsonStore.query(filter).count().subscribe(new Action1<Integer>() {

                    @Override
                    public void call(Integer numberOfFeatures) {
                        assertEquals(16, (int) numberOfFeatures);
                    }

                });
            }
        });

    }

}

