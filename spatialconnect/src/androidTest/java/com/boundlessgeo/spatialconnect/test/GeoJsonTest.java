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

import rx.functions.Action1;


public class GeoJsonTest extends BaseTestCase {

    private SCServiceManager serviceManager;
    private final static String GEOJSON_STORE_ID = "50402599-3ad3-439f-9c49-3c8a7579933b";


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        serviceManager = new SCServiceManager(activity);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        testContext.deleteFile("nola_polling_places.json");
    }

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

