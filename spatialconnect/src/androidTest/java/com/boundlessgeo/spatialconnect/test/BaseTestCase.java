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

import android.app.Activity;
import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.boundlessgeo.spatialconnect.SpatialConnectActivity;
import com.boundlessgeo.spatialconnect.stores.FormStore;
import com.boundlessgeo.spatialconnect.stores.LocationStore;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
    protected static File localConfigFile;

    protected final static String RIO_GPKG_ID = "9d415438-08c5-48f5-b80b-d5bc9c810b3d";
    protected final static String HAITI_GPKG_ID = "c96c0155-31b3-434a-8171-beb36fb24512";
    protected final static String WHITEHORSE_GPKG_ID = "deba6ab8-016e-4e0a-8010-c2e77d72d0a1";
    protected final static String POLYGONS_GPKG_ID = "45d45141-e1a3-4044-95f0-7d4a37cf23e1";
    protected final static String BARS_GEO_JSON_ID = "3dc5afc9-393b-444c-8581-582e2c2d98a3";

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
        try {
            localConfigFile = File.createTempFile("config_local.scfg", null, activity.getCacheDir());

            // read test remote.scfg file from test resources directory
            //set local test config
            InputStream is = testContext.getResources().openRawResource(R.raw.config);
            FileOutputStream fos = new FileOutputStream(localConfigFile);
            byte[] data = new byte[is.available()];
            is.read(data);
            fos.write(data);

            is.close();
            fos.close();
        } catch (IOException ex) {
            System.exit(0);
        }
    }

   protected static void deleteDatabases() {
       testContext.deleteDatabase(POLYGONS_GPKG_ID);
       testContext.deleteDatabase(HAITI_GPKG_ID);
       testContext.deleteDatabase(WHITEHORSE_GPKG_ID);
       testContext.deleteDatabase(FormStore.NAME);
       testContext.deleteDatabase(LocationStore.NAME);
   }

}
