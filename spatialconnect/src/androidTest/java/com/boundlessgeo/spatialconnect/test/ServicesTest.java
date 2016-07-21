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
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ServicesTest extends BaseTestCase {

    @Test
    public void testDataServiceInitialization() {
        SpatialConnect.getInstance().initialize(testContext);
        SCDataService dataService = SpatialConnect.getInstance().getDataService();
        assertEquals("The data service should have 4 supported stores.",
                4, dataService.getSupportedStoreKeys().size());
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
        assertEquals("6 default services should have been initialized (data, network, config, sensor, auth, and " +
                        "kvpstore)",
                6, sc.getServices().size()
        );
        assertEquals("There should be only 4 supported data stores: geojson.1, gpkg.1 and geojson.1.0, gpkg.1.0",
                4,
                sc.getDataService().getSupportedStoreKeys().size()
        );
        assertTrue("The geojson.1 store should be in the list of supported stores.",
                sc.getDataService().getSupportedStoreKeys().contains("geojson.1")
        );
        assertTrue("The gpkg.1 store should be in the list of supported stores.",
                sc.getDataService().getSupportedStoreKeys().contains("gpkg.1")
        );
    }
}
