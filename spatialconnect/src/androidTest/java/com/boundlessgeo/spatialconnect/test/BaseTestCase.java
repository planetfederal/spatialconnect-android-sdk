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
import android.test.ActivityInstrumentationTestCase2;

import com.boundlessgeo.spatialconnect.SpatialConnectActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BaseTestCase extends ActivityInstrumentationTestCase2<SpatialConnectActivity> {

    /**
     * Activity
     */
    protected Activity activity = null;

    /**
     * Test context
     */
    protected Context testContext = null;

    /**
     * Test context
     */
    protected File testConfigFile;

    /**
     * Constructor
     */
    public BaseTestCase() {
        super(SpatialConnectActivity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Set the activity and test context
        activity = getActivity();
        testContext = activity.createPackageContext(
                "com.boundlessgeo.spatialconnect.test",
                Context.CONTEXT_IGNORE_SECURITY
        );
        try {
            testConfigFile = File.createTempFile("config.scfg", null, activity.getCacheDir());
            // read test scconfig.json file from test resources directory
            InputStream is = testContext.getResources().openRawResource(R.raw.scconfig);
            FileOutputStream fos = new FileOutputStream(testConfigFile);
            byte[] data = new byte[is.available()];
            is.read(data);
            fos.write(data);
            is.close();
            fos.close();
        } catch (IOException ex) {
            System.exit(0);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
