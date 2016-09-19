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
import com.boundlessgeo.spatialconnect.services.SCBackendService;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.functions.Func1;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;

public class SCConfigServiceTest extends BaseTestCase {

    @Before
    public void beforeTest() throws Exception {
        deleteDatabases();
    }

    @After
    public void afterTest() throws Exception {
        SpatialConnect.getInstance().stopAllServices();
        deleteDatabases();
    }

    @Test
    public void testSpatialConnectCanLoadLocalConfigs() {
        SpatialConnect sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(localConfigFile);
        sc.startAllServices();
        waitForStoreToStart(GEOJSON_STORE_ID, sc);
        assertEquals("The test config file has 4 stores (1 from the local config plus form, default, and location " +
                "stores.",
                4, sc.getDataService().getAllStores().size());
    }

    @Test
    public void testConfigServiceCanLoadConfigsFromBackendService() {
        SpatialConnect sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(remoteConfigFile);
        sc.startAllServices();
        sc.getAuthService().authenticate("admin@something.com", "admin");
        TestSubscriber testSubscriber = new TestSubscriber();
        SCBackendService.configReceived
                .filter(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean b) {
                        return b;
                    }
                })
                .take(1)
                .timeout(15, TimeUnit.SECONDS)
                .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertValues(true);
        waitForStoreToStart(RIO_GPKG_ID, sc);
        assertEquals("The remote config file defines 3 stores; plus 7 from the remote config; plus the form, default," +
                " and location stores",
                13, sc.getDataService().getAllStores().size());
        assertEquals("The stores from the remote config are running.",
                SCDataStoreStatus.SC_DATA_STORE_RUNNING, sc.getDataService().getStoreById(RIO_GPKG_ID).getStatus());
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
