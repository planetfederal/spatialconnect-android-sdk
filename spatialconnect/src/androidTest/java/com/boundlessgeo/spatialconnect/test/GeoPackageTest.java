package com.boundlessgeo.spatialconnect.test;


import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCKeyTuple;

import java.util.Random;

import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import rx.functions.Action1;


public class GeoPackageTest extends BaseTestCase {

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
        testContext.deleteDatabase("haiti-vectors-split.gpkg");
    }

    public void testThatDataServiceStartedGeoPackageStore() {
        boolean containsGeoPackageStore = false;
        for (SCDataStore store : serviceManager.getDataService().getActiveStores()) {
            assertTrue("The store should be running.",
                    store.getStatus().equals(SCDataStoreStatus.DATA_STORE_RUNNING)
            );
            if (store.getType().equals("geopackage")) {
                containsGeoPackageStore = true;
                assertTrue("The store's adapter should be connected",
                        store.getAdapter().getStatus()
                                .equals(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED)
                );
            }
        }
        assertTrue("A geopackage store should be active.", containsGeoPackageStore);
    }

    public void testGeoPackageQuery() {
        // this is the geopackage located http://www.geopackage.org/data/haiti-vectors-split.gpkg
        SCDataStore gpkgStore = serviceManager.getDataService().getStoreById("1234");
        SCQueryFilter filter = new SCQueryFilter();
        // bbox around part of haiti
        SCBoundingBox bbox = new SCBoundingBox(-73.3901, 18.6261, -72.5097, 19.1627);
        filter.setPredicate(
                new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
        );
        /** test that there are 1871 features returned.  the reference queryFeature to find this number is:
         SELECT(
         (SELECT COUNT(*) FROM point_features  WHERE ST_Within(the_geom, ST_GeomFromText('POLYGON((-73.3901 19.1627, -72.5097 19.1627, -72.5097 18.6261, -73.3901 18.6261, -73.3901 19.1627))')))+
         (SELECT COUNT(*) FROM polygon_features  WHERE ST_Within(the_geom, ST_GeomFromText('POLYGON((-73.3901 19.1627, -72.5097 19.1627, -72.5097 18.6261, -73.3901 18.6261, -73.3901 19.1627))')))+
         (SELECT COUNT(*) FROM linear_features  WHERE ST_Within(the_geom, ST_GeomFromText('POLYGON((-73.3901 19.1627, -72.5097 19.1627, -72.5097 18.6261, -73.3901 18.6261, -73.3901 19.1627))')))
         ) AS totalFeatures;
         */
        gpkgStore.query(filter).count().subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer numberOfFeatures) {
                assertEquals(new Integer(1871), numberOfFeatures);
            }

        });

        gpkgStore.query(filter).subscribe(new Action1<SCSpatialFeature>() {
            @Override
            public void call(SCSpatialFeature feature) {
                assertEquals("The store id should be the first part of the id",
                        "1234",
                        feature.getId().split("\\.")[0]
                );
                assertEquals("There should be 3 periods in the id.",
                        3,
                        feature.getId().split("\\.").length
                );
                assertTrue("There should be properties in the properties map.",
                        feature.getProperties().size() > 0
                );
            }
        });
    }

    public void testGeoPackageCreateFeature() {
        SCDataStore gpkgStore = serviceManager.getDataService().getStoreById("1234");
        SCSpatialFeature scSpatialFeature = new SCSpatialFeature();
        // TODO: how should we handle new feature ids...just put a -1 or put nothing?
        scSpatialFeature.setId("1234.point_features.5555");
        scSpatialFeature.getProperties().put("prop1", "prop1_value");

        gpkgStore.create(scSpatialFeature).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean created) {
                assertTrue("The feature should have been created", created);
            }
        });
    }

    public void testGeoPackageUpdateFeature() {
        Random rand = new Random();
        final String randomId = String.valueOf(rand.nextInt((100)));
        final String featureId = "1234.point_features." + randomId;
        final SCDataStore gpkgStore = serviceManager.getDataService().getStoreById("1234");
        final SCSpatialFeature scSpatialFeature = new SCSpatialFeature();
        // try to update the first feature from the haiti sample geopackage
        scSpatialFeature.setId(featureId);
        scSpatialFeature.getProperties().put("featureid", "featureid_value");

        gpkgStore.update(scSpatialFeature).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean updated) {
                assertTrue("The feature should have been updated", updated);
                FeatureDao featureDao = ((GeoPackageStore) gpkgStore).getFeatureDao(featureId);
                FeatureRow featureRow = featureDao.queryForIdRow(Long.valueOf(randomId));
                assertEquals("The value of the 'featureid' column should have been updated.",
                        "featureid_value",
                        featureRow.getValue("featureid")
                );
            }
        });
    }

    public void testGeoPackageDeleteFeature() {
        final String featureId = "1234.point_features.3";
        final SCDataStore gpkgStore = serviceManager.getDataService().getStoreById("1234");
        SCSpatialFeature scSpatialFeature = new SCSpatialFeature();
        scSpatialFeature.setId(featureId);
        SCKeyTuple t = scSpatialFeature.getKey();
        gpkgStore.delete(t).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean deleted) {
                assertTrue("The featureRow should have been deleted", deleted);
                FeatureDao featureDao = ((GeoPackageStore) gpkgStore).getFeatureDao(featureId);
                assertTrue("The result should be null b/c the feature should have been deleted",
                        featureDao.queryForIdRow(3) == null
                );
            }
        });
    }
}

