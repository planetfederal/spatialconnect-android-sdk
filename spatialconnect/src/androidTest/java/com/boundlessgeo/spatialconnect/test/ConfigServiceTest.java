/*
 * Copyright 2016 Boundless, http://boundlessgeo.com
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

import com.boundlessgeo.spatialconnect.db.SCKVPStore;
import com.boundlessgeo.spatialconnect.db.SCStoreConfigRepository;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;

public class ConfigServiceTest extends BaseTestCase {

    private SCServiceManager manager;
    private final static String HAITI_GPKG_ID = "a5d93796-5026-46f7-a2ff-e5dec85heh6b";

    @Before
    public void beforeTest() throws Exception {
        testContext.deleteDatabase("Haiti");
        testContext.deleteDatabase("Whitehorse");
        testContext.deleteDatabase(SCKVPStore.DATABASE_NAME);
    }


    @Test
    public void testServiceManagerCanLoadNonDefaultConfigs() {
        manager = new SCServiceManager(testContext);
        manager.addConfig(testConfigFile);
        manager.startAllServices();
        assertEquals("The test config file has 3 stores.",
                3, manager.getDataService().getAllStores().size());
    }

    @Test @Ignore // ignoring until the network service is implemented
    public void testConfigServiceLoadsConfigsThroughServiceManager() {
        manager = new SCServiceManager(testContext);
        manager.startAllServices();
        manager.loadDefaultConfigs();
        assertEquals("The remote config has 2 stores.  We're not packaging any configs on the sdcard so only the " +
                        "remote config has stores that are loaded.",
                2, manager.getDataService().getAllStores().size());
    }

    @Test
    public void testConfigServicePersistsConfigs() {
        manager = new SCServiceManager(testContext);
        manager.startAllServices();
        manager.loadDefaultConfigs();
        waitForStoreToStart(HAITI_GPKG_ID);
        SCStoreConfigRepository stores = new SCStoreConfigRepository(testContext);
        assertEquals("Config service should have persisted 2 stores (from the remote location).",
                2, stores.getNumberOfStores());
    }

    @Ignore @Test
    public void testLibGpkgFunctionsLoaded() {
        manager = new SCServiceManager(testContext);
        manager.startAllServices();
//        waitForAllStoresToStart();
//                .executeSql("SELECT ST_AsText(the_geom) FROM point_features LIMIT 1;");
//                .executeSql("SELECT CreateSpatialIndex('point_features', 'the_geom', 'id');");
    }


    // TODO: test erroneous config files

    private void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        manager.getDataService().storeStarted(storeId).timeout(5, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }
}
