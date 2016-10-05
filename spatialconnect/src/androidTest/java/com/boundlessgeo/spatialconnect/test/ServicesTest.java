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

import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;

import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ServicesTest extends BaseTestCase {

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        deleteDatabases();
    }

    @Test
    public void testDataServiceInitialization() {
        SpatialConnect.getInstance().initialize(testContext);
        SCDataService dataService = SpatialConnect.getInstance().getDataService();
        assertEquals("The data service should have 5 supported stores.",
                5, dataService.getSupportedStoreKeys().size());
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
        assertEquals("5 default services should have been initialized (data, kvp, sensor, config, and auth)",
                5, sc.getServices().size()
        );
        assertEquals("There should be only 5 supported data stores: geojson.1, gpkg.1 and geojson.1.0, gpkg.1.0, " +
                        "wfs.1.1.0",
                5,
                sc.getDataService().getSupportedStoreKeys().size()
        );
        assertTrue("The geojson.1 store should be in the list of supported stores.",
                sc.getDataService().getSupportedStoreKeys().contains("geojson.1")
        );
        assertTrue("The gpkg.1 store should be in the list of supported stores.",
                sc.getDataService().getSupportedStoreKeys().contains("gpkg.1")
        );
        assertTrue("The wfs.1.1.0 store should be in the list of supported stores.",
                sc.getDataService().getSupportedStoreKeys().contains("wfs.1.1.0")
        );
    }
}
