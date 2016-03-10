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

import com.boundlessgeo.spatialconnect.services.SCConfigService;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ConfigServiceTest extends BaseTestCase {

    @Test
    public void testConfigServiceCanLoadConfigsFromExternalStorage() {
        SCConfigService configService = new SCConfigService(testContext);
        configService.loadConfigs(testConfigFile);
        assertEquals("The test config file has 3 stores.", 3, SCDataService.getInstance().getAllStores().size());
    }

    @Test
    public void testConfigServiceLoadsConfigsThroughServiceManager() {
        SCServiceManager manager = new SCServiceManager(testContext);
        // the test config packaged with the app has 3 stores.  The remote config has 1 of the same stores plus 1
        // additional store not defined in the test config.
        assertEquals("The test config file has 3 stores plus another distinct store from the remote config",
                4, SCDataService.getInstance().getAllStores().size());
    }

    @Test
    public void testConfigServiceLoadsConfigsThroughServiceManagerWithOptionalConstructor() {
        SCServiceManager manager = new SCServiceManager(testContext, testConfigFile);
        assertEquals("It should only have loaded the 3 stores from the config file.",
                3, SCDataService.getInstance().getAllStores().size());
    }

    // TODO: test erroneous config files
}
