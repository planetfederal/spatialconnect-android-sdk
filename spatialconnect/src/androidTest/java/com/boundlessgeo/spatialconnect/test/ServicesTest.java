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
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;

import org.junit.AfterClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ServicesTest extends BaseTestCase {

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        deleteDatabases();
    }

    @Test
    public void testSpatialConnectSetup() {
        SpatialConnect.getInstance().initialize(testContext);
        SCDataService dataService = SpatialConnect.getInstance().getDataService();
        for (SCDataStore store : dataService.getAllStores()) {
            new SCDataService(testContext).unregisterStore(store);
        }
        SpatialConnect sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        assertEquals("4 default services should have been initialized (data, sensor, config, and auth) no backend svc for unit test",
                4, sc.getServices().size()
        );
    }
}
