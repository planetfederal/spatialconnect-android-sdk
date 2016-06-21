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

import android.app.Activity;
import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.boundlessgeo.spatialconnect.SpatialConnectActivity;
import com.boundlessgeo.spatialconnect.db.SCKVPStore;
import com.boundlessgeo.spatialconnect.stores.DefaultStore;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;


import java.io.File;

@RunWith(AndroidJUnit4.class)
public abstract class BaseTestCase {

    /**
     * Activity
     */
    protected static Activity activity = null;

    /**
     * Test context
     */
    protected static Context testContext = null;

    /**
     * Test context
     */
    protected static File testConfigFile;

    protected final static String HAITI_GPKG_ID = "f6dcc750-1349-46b9-a324-0223764d46d1";
    protected final static String WHITEHORSE_GPKG_ID = "fad33ae1-f529-4c79-affc-befc37c104ae";
    protected final static String GEOJSON_STORE_ID = "50402599-3ad3-439f-9c49-3c8a7579933b";

    public BaseTestCase() {
    }

    @ClassRule
    public static ActivityTestRule<SpatialConnectActivity> rule = new ActivityTestRule<>(SpatialConnectActivity.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Set the activity and test context
        activity = rule.getActivity();
        testContext = activity.createPackageContext(
                "com.boundlessgeo.spatialconnect.test",
                Context.CONTEXT_IGNORE_SECURITY
        );
    }

   protected static void deleteDatabases() {
       testContext.deleteDatabase("gpkg1");
       testContext.deleteDatabase("gpkg2");
       testContext.deleteDatabase(SCKVPStore.DATABASE_NAME);
       testContext.deleteDatabase(DefaultStore.NAME);
   }

}
