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
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreException;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCStoreStatusEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;


public class GeoPackageTest extends BaseTestCase {

    private SCServiceManager serviceManager;
    private final static String HAITI_GPKG_ID = "a5d93796-5026-46f7-a2ff-e5dec85heh6b";
    private final static String WHITEHORSE_GPKG_ID = "ba293796-5026-46f7-a2ff-e5dec85heh6b";

    @Before
    public void setUp() throws Exception {
        serviceManager = new SCServiceManager(activity, testConfigFile);
        serviceManager.startAllServices();
    }

    @After
    public void tearDown() throws Exception {
        testContext.deleteDatabase("Haiti");
        testContext.deleteDatabase("Whitehorse");
    }

    @Test
    public void testThatDataServiceStartedGeoPackageStore() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
                boolean containsGeoPackageStore = false;
                for (SCDataStore store : serviceManager.getDataService().getActiveStores()) {
                    assertTrue("The store should be running.",
                            store.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)
                    );
                    if (store.getType().equals("gpkg")) {
                        containsGeoPackageStore = true;
                        assertTrue("The store's adapter should be connected",
                                store.getAdapter().getStatus()
                                        .equals(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED)
                        );
                    }
                }
                assertTrue("A geopackage store should be running.", containsGeoPackageStore);
            }
        });
    }

    @Test
    public void testGeoPackageQueryWithin() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
                // this is the geopackage located http://www.geopackage.org/data/haiti-vectors-split.gpkg
                SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(HAITI_GPKG_ID);

                // bbox around part of haiti
                SCBoundingBox bbox = new SCBoundingBox(-73.3901, 18.6261, -72.5097, 19.1627);
                SCQueryFilter filter = new SCQueryFilter(
                        new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
                );
                /** test that there are 1871 features returned.  the reference queryFeature to find this number is:
                 SELECT(
                 (SELECT COUNT(*) FROM point_features  WHERE ST_Within(the_geom, ST_GeomFromText('POLYGON((-73.3901 19.1627, -72.5097 19.1627, -72.5097 18.6261, -73.3901 18.6261, -73.3901 19.1627))')))+
                 (SELECT COUNT(*) FROM polygon_features  WHERE ST_Within(the_geom, ST_GeomFromText('POLYGON((-73.3901 19.1627, -72.5097 19.1627, -72.5097 18.6261, -73.3901 18.6261, -73.3901 19.1627))')))+
                 (SELECT COUNT(*) FROM linear_features  WHERE ST_Within(the_geom, ST_GeomFromText('POLYGON((-73.3901 19.1627, -72.5097 19.1627, -72.5097 18.6261, -73.3901 18.6261, -73.3901 19.1627))')))
                 ) AS totalFeatures;
                 */

                assertNotNull("The store should downloaded locally", gpkgStore);

                gpkgStore.query(filter).count().subscribe(new Action1<Integer>() {

                    @Override
                    public void call(Integer numberOfFeatures) {
                        assertTrue(numberOfFeatures > 0);
                    }

                });

                gpkgStore.query(filter).take(1).subscribe(new Action1<SCSpatialFeature>() {
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
        });
    }

    @Test
    public void testGeoPackageQueryContains() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
                // this is the geopackage located http://www.geopackage.org/data/haiti-vectors-split.gpkg
                SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(HAITI_GPKG_ID);

                // bbox around part of haiti
                SCBoundingBox bbox = new SCBoundingBox(-73.3901, 18.6261, -72.5097, 19.1627);
                SCQueryFilter filter = new SCQueryFilter(
                        new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_CONTAINS)
                );
                /** test that there are 1871 features returned.  the reference queryFeature to find this number is:
                 SELECT(
                 (SELECT COUNT(*) FROM point_features  WHERE ST_Within(the_geom, ST_GeomFromText('POLYGON((-73.3901 19.1627, -72.5097 19.1627, -72.5097 18.6261, -73.3901 18.6261, -73.3901 19.1627))')))+
                 (SELECT COUNT(*) FROM polygon_features  WHERE ST_Within(the_geom, ST_GeomFromText('POLYGON((-73.3901 19.1627, -72.5097 19.1627, -72.5097 18.6261, -73.3901 18.6261, -73.3901 19.1627))')))+
                 (SELECT COUNT(*) FROM linear_features  WHERE ST_Within(the_geom, ST_GeomFromText('POLYGON((-73.3901 19.1627, -72.5097 19.1627, -72.5097 18.6261, -73.3901 18.6261, -73.3901 19.1627))')))
                 ) AS totalFeatures;
                 */

                assertNotNull("The store should downloaded locally", gpkgStore);

                gpkgStore.query(filter).count().subscribe(new Action1<Integer>() {

                    @Override
                    public void call(Integer numberOfFeatures) {
                        assertTrue(numberOfFeatures > 0);
                    }

                });
            }
        });
    }

    @Test
    public void testGeoPackageCreateFeature() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
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
        });
    }

    @Test
    public void testGeoPackageUpdateFeature() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
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
        });
    }

    @Test
    public void testGeoPackageDeleteFeature() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
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
        });
    }

    @Test
    public void testGeoPackageCreateFeatureThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
                SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(WHITEHORSE_GPKG_ID);
                TestSubscriber testSubscriber = new TestSubscriber();
                gpkgStore.create(getTestHaitiPoint()).subscribe(testSubscriber);
                testSubscriber.assertError(SCDataStoreException.class);
                assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                        SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                        ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
                );
            }
        });
    }

    @Test
    public void testGeoPackageUpdateFeatureThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
                SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(WHITEHORSE_GPKG_ID);
                TestSubscriber testSubscriber = new TestSubscriber();
                gpkgStore.update(getTestHaitiPoint()).subscribe(testSubscriber);
                testSubscriber.assertError(SCDataStoreException.class);
                assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                        SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                        ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
                );
            }
        });
    }

    @Test
    public void testGeoPackageDeleteFeatureThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
                SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(WHITEHORSE_GPKG_ID);
                TestSubscriber testSubscriber = new TestSubscriber();
                gpkgStore.delete(getTestHaitiPoint().getKey()).subscribe(testSubscriber);
                testSubscriber.assertError(SCDataStoreException.class);
                assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                        SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                        ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
                );
            }
        });
    }

    @Test
    public void testGeoPackageQueryByIdThrowsDataStoreExceptionWhenNoFeatureTablesExist() {
        serviceManager.getDataService().allStoresStartedObs().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent scStoreStatusEvent) {
                SCDataStore gpkgStore = serviceManager.getDataService().getStoreById(WHITEHORSE_GPKG_ID);
                TestSubscriber testSubscriber = new TestSubscriber();
                gpkgStore.queryById(getTestHaitiPoint().getKey()).subscribe(testSubscriber);
                testSubscriber.assertError(SCDataStoreException.class);
                assertEquals("The exception should be of type LAYER_NOT_FOUND.",
                        SCDataStoreException.ExceptionType.LAYER_NOT_FOUND,
                        ((SCDataStoreException) testSubscriber.getOnErrorEvents().get(0)).getType()
                );
            }
        });
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

