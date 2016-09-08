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
        waitForStoreToStart(RIO_GPKG_ID, sc);
        assertEquals("The test config file has 5 stores (3 from the local config + form and default stores ",
                5, sc.getDataService().getAllStores().size());
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
                .filter(new Func1<Integer, Boolean>() {
                    @Override
                    public Boolean call(Integer integer) {
                        return integer == 1;
                    }
                })
                .take(1)
                .timeout(15, TimeUnit.SECONDS)
                .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertValues(1);
        waitForStoreToStart(RIO_GPKG_ID, sc);
        assertEquals(
            "The remote config file has 6 stores plus the form, location, and default stores",
            9,
            sc.getDataService().getAllStores().size()
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
