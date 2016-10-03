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
 * See the License for the specific language governing permissions and limitations under the
 * License
 */
package com.boundlessgeo.spatialconnect.test;

import android.app.Activity;
import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import com.boundlessgeo.spatialconnect.SpatialConnectActivity;
import com.boundlessgeo.spatialconnect.db.SCKVPStore;
import com.boundlessgeo.spatialconnect.stores.DefaultStore;
import com.boundlessgeo.spatialconnect.stores.FormStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class) public abstract class BaseTestCase {

  protected final static String RIO_GPKG_ID = "5729cac9-cf37-476a-997d-f9c687b4df67";
  protected final static String HAITI_GPKG_ID = "27ed943c-8fc9-4d18-a7f0-4dfc03a460d6";
  protected final static String WHITEHORSE_GPKG_ID = "fad33ae1-f529-4c79-affc-befc37c104ae";
  protected final static String GEOJSON_STORE_ID = "50402599-3ad3-439f-9c49-3c8a7579933b";
  @ClassRule public static ActivityTestRule<SpatialConnectActivity> rule =
      new ActivityTestRule<>(SpatialConnectActivity.class);
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
  protected static File remoteConfigFile;
  protected static File localConfigFile;

  public BaseTestCase() {
  }

  @BeforeClass public static void setUpBeforeClass() throws Exception {
    // Set the activity and test context
    activity = rule.getActivity();
    testContext = activity.createPackageContext("com.boundlessgeo.spatialconnect.test",
        Context.CONTEXT_IGNORE_SECURITY);
    try {
      remoteConfigFile = File.createTempFile("config_remote.scfg", null, activity.getCacheDir());
      localConfigFile = File.createTempFile("config_local.scfg", null, activity.getCacheDir());

      // read test scconfig_remote.json file from test resources directory
      //set local test config
      InputStream is = testContext.getResources().openRawResource(R.raw.scconfig_local);
      FileOutputStream fos = new FileOutputStream(localConfigFile);
      byte[] data = new byte[is.available()];
      is.read(data);
      fos.write(data);

      //set remote test config
      is = testContext.getResources().openRawResource(R.raw.scconfig_remote);
      fos = new FileOutputStream(remoteConfigFile);
      data = new byte[is.available()];

      is.read(data);
      fos.write(data);

      is.close();
      fos.close();
    } catch (IOException ex) {
      System.exit(0);
    }
  }

  protected static void deleteDatabases() {
    testContext.deleteDatabase("Rio");
    testContext.deleteDatabase("Haiti");
    testContext.deleteDatabase("Whitehorse");
    testContext.deleteDatabase(SCKVPStore.DATABASE_NAME);
    testContext.deleteDatabase(DefaultStore.NAME);
    testContext.deleteDatabase(FormStore.NAME);
  }
}
