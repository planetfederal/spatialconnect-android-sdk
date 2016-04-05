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

import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ServicesTest extends BaseTestCase {

    @Test
    public void testDataServiceInitialization() {
        SCDataService dataService = SCDataService.getInstance();
        assertTrue("The data service should have 2 supported stores.", dataService.getSupportedStoreKeys().size() == 2);
    }

    @Test
    public void testServiceManagerSetup() {
        for (SCDataStore store : SCDataService.getInstance().getAllStores()) {
            SCDataService.getInstance().unregisterStore(store);
        }
        SCServiceManager serviceManager = new SCServiceManager(testContext);
        assertEquals("4 default services should have been initialized (data, network, config, and kvpstore)",
                4, serviceManager.getServices().size()
        );
        assertEquals("There should be only 2 supported data stores: geojson.1 and gpkg.1",
                2,
                serviceManager.getDataService().getSupportedStoreKeys().size()
        );
        assertTrue("The geojson.1 store should be in the list of supported stores.",
                serviceManager.getDataService().getSupportedStoreKeys().contains("geojson.1")
        );
        assertTrue("The gpkg.1 store should be in the list of supported stores.",
                serviceManager.getDataService().getSupportedStoreKeys().contains("gpkg.1")
        );
    }

    @Test
    public void testAllStoresStartedObsCompletesWithNoErrors() {
        SCServiceManager serviceManager = new SCServiceManager(activity);
        serviceManager.startAllServices();
        TestSubscriber testSubscriber = new TestSubscriber();
        // timeout if all stores don't start in 5 minutes
        serviceManager.getDataService().allStoresStartedObs().timeout(5, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        if (testSubscriber.getOnErrorEvents().size() > 0) {
            System.out.println("The error was: " + ((Throwable)testSubscriber.getOnErrorEvents().get(0)).getMessage());
        }
        testSubscriber.assertNoValues();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }
}
