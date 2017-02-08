/**
 * Copyright 2015-2017 Boundless, http://boundlessgeo.com
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
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.services.SCConfigService;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCServiceStatus;
import com.boundlessgeo.spatialconnect.services.SCServiceStatusEvent;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rx.functions.Action0;
import rx.functions.Action1;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class SCServiceTest extends BaseTestCase {
    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.getConfigService().addConfigFilePath(localConfigFile.getAbsolutePath());
        sc.startAllServices();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        sc.stopAllServices();
    }

    @Test
    public void testSensorService() {
        sc.serviceRunning(SCSensorService.serviceId()).subscribe(new Action1<SCServiceStatusEvent>() {
            @Override
            public void call(SCServiceStatusEvent scServiceStatusEvent) {}
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {}
        }, new Action0() {
            @Override
            public void call() {
                assertNotNull(sc.getSensorService());
                assertTrue(sc.getSensorService().getStatus() == SCServiceStatus.SC_SERVICE_RUNNING);
            }
        });
    }

    @Test
    public void testDataService() {
        sc.serviceRunning(SCDataService.serviceId()).subscribe(new Action1<SCServiceStatusEvent>() {
            @Override
            public void call(SCServiceStatusEvent scServiceStatusEvent) {}
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {}
        }, new Action0() {
            @Override
            public void call() {
                //check dependencies are running
                assertNotNull(sc.getSensorService());
                assertTrue(sc.getSensorService().getStatus() == SCServiceStatus.SC_SERVICE_RUNNING);

                //check data service is running
                assertNotNull(sc.getDataService());
                assertTrue(sc.getDataService().getStatus() == SCServiceStatus.SC_SERVICE_RUNNING);
            }
        });
    }

    @Test
    public void testStartWithDependenciesService() {
        sc.serviceRunning(SCDataService.serviceId()).subscribe(new Action1<SCServiceStatusEvent>() {
            @Override
            public void call(SCServiceStatusEvent scServiceStatusEvent) {}
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {}
        }, new Action0() {
            @Override
            public void call() {
                //check dependencies are running
                assertNotNull(sc.getDataService());
                assertTrue(sc.getDataService().getStatus() == SCServiceStatus.SC_SERVICE_RUNNING);

                //check config service is running
                assertNotNull(sc.getConfigService());
                assertTrue(sc.getConfigService().getStatus() == SCServiceStatus.SC_SERVICE_RUNNING);
            }
        });
    }

    @Test
    public void testStopWithDependenciesService() {
        sc.serviceRunning(SCDataService.serviceId()).subscribe(new Action1<SCServiceStatusEvent>() {
            @Override
            public void call(SCServiceStatusEvent scServiceStatusEvent) {}
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {}
        }, new Action0() {
            @Override
            public void call() {
                sc.stopService(SCConfigService.serviceId());

                //check dependencies are stopped
                assertNotNull(sc.getDataService());
                assertTrue(sc.getDataService().getStatus() == SCServiceStatus.SC_SERVICE_STOPPED);

                //check config service is stopped
                assertNotNull(sc.getConfigService());
                assertTrue(sc.getConfigService().getStatus() == SCServiceStatus.SC_SERVICE_STOPPED);
            }
        });
    }
}
