package com.boundlessgeo.spatialconnect.test;


import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;

import rx.functions.Action1;


public class GeoJsonTest extends BaseTestCase {

    private SCServiceManager serviceManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        serviceManager = new SCServiceManager(activity);
        serviceManager.startAllServices();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        testContext.deleteFile("nola_polling_places.json");
    }

    public void testSearchGeoJsonStore() {
        SCDataStore geoJsonStore = serviceManager.getDataService().getStoreById("504");
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

}

