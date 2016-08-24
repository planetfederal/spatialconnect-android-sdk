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

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.db.SCStoreConfigRepository;
import com.boundlessgeo.spatialconnect.services.SCConfigService;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SCConfigServiceTest extends BaseTestCase {

    @Before
    public void beforeTest() throws Exception {
        deleteDatabases();
    }

    @After
    public void afterTest() throws Exception {
        deleteDatabases();
    }

    @Test
    @Ignore
    public void testSpatialConnectCanLoadNonDefaultConfigs() {
        SpatialConnect sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(remoteConfigFile);
        sc.startAllServices();
        waitForStoreToStart(WHITEHORSE_GPKG_ID, sc);
        assertEquals("The test config file has 3 stores.", 3, sc.getDataService().getAllStores().size());
    }

    @Test
    @Ignore
    public void testConfigServicePersistsConfigs() {
        SpatialConnect sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.startAllServices();
        // the remote config doesn't have the whitehorse gpkg so we wait for haiti
        waitForStoreToStart(HAITI_GPKG_ID, sc);
        SCStoreConfigRepository stores = new SCStoreConfigRepository(testContext);
        assertEquals("Config service should have persisted 2 stores (from the remote location).",
                2, stores.getNumberOfStores());
    }

    @Test
    public void testConfigServiceReadsMqttBrokerUri() {
        SpatialConnect sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(testConfigFile);
        sc.startAllServices();
        sc.getAuthService().authenticate("admin@something.com", "admin");
        SCConfigService configService = sc.getConfigService();
        assertTrue("The mqtt broker uri should not be null b/c it is defined in the test config.",
                null != configService.getMqttBrokerUri()
        );
        assertEquals("The mqtt broker uri should match the one in the config.",
                "tcp://192.168.99.100:1883",
                configService.getMqttBrokerUri()
        );
    }

    // TODO: test erroneous config files

    private void waitForStoreToStart(final String storeId, SpatialConnect sc) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(5, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }
}
