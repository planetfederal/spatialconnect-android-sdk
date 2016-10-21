package com.boundlessgeo.spaconapp.test;

import android.app.Activity;
import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.boundlessgeo.spaconapp.MainActivity;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.SpatialConnectActivity;
import com.boundlessgeo.spatialconnect.config.SCConfig;
import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.mqtt.MqttHandler;
import com.boundlessgeo.spatialconnect.schema.SCMessageOuterClass;
import com.boundlessgeo.spatialconnect.services.SCBackendService;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class IntegratedTests {

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

    private static SpatialConnect sc;

    @ClassRule
    public static ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Set the activity and test context
        activity = rule.getActivity();
        testContext = activity.createPackageContext(
                "com.boundlessgeo.spaconapp.test",
                Context.CONTEXT_IGNORE_SECURITY
        );
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

            sc = SpatialConnect.getInstance();
            sc.initialize(activity);
            sc.addConfig(remoteConfigFile);
            sc.startAllServices();
            sc.getAuthService().authenticate("admin@something.com", "admin");

        } catch (IOException ex) {
            System.exit(0);
        }
    }

    @After
    public void afterTest() throws Exception {
        sc.stopAllServices();
    }

    @Test
    public void testCRUDonRemoteConfigCache() {

        SCBackendService.configReceived.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                //CREATE
                SCStoreConfig newStore = new SCStoreConfig();
                newStore.setType("wfs");
                newStore.setUniqueID("1e7ef7a7-85a8-4056-9f65-88bd0be1520d");
                newStore.setVersion("1");
                newStore.setUri("http://efc-dev.boundlessgeo.com:8080/geoserver/spatialconnect/ows");
                newStore.setName("marcs birds");

                sc.getConfigService().addStoreConfigCache(newStore);

                boolean newStoreFound = false;

                SCConfig cachedConfig = sc.getConfigService().getConfigFromCache();

                for (SCStoreConfig cachedStore : cachedConfig.getStores()) {
                    if (newStore.getUniqueID().equalsIgnoreCase(cachedStore.getUniqueID()) &&
                            newStore.getUri().equalsIgnoreCase(cachedStore.getUri()) &&
                            newStore.getName().equalsIgnoreCase(cachedStore.getName())) {
                        newStoreFound = true;
                        break;
                    }
                }

                assertTrue("New Store added to remote config cache", newStoreFound);

                //UPDATE
                SCStoreConfig modifiedStore = new SCStoreConfig();
                modifiedStore.setType("wfs");
                modifiedStore.setUniqueID("1e7ef7a7-85a8-4056-9f65-88bd0be1520d");
                modifiedStore.setVersion("2");
                modifiedStore.setUri("http://efc-dev.boundlessgeo.com:8080/geoserver/spatialconnect/ows");
                modifiedStore.setName("Birds");

                boolean storeModifed = false;

                sc.getConfigService().updateStoreConfigCache(modifiedStore);

                cachedConfig = sc.getConfigService().getConfigFromCache();

                for (SCStoreConfig cachedStore : cachedConfig.getStores()) {
                    if (modifiedStore.getUniqueID().equalsIgnoreCase(cachedStore.getUniqueID()) &&
                            modifiedStore.getName().equalsIgnoreCase("Birds")) {
                        storeModifed = true;
                        break;
                    }
                }

                assertTrue("Store updated in remote config cache", storeModifed);

                //DELETE
                sc.getConfigService().removeStoreConfigFromCache("1e7ef7a7-85a8-4056-9f65-88bd0be1520d");

                cachedConfig = sc.getConfigService().getConfigFromCache();
                boolean removedStoreFound = false;
                for (SCStoreConfig cachedStore : cachedConfig.getStores()) {
                    if (modifiedStore.getUniqueID().equalsIgnoreCase(cachedStore.getUniqueID())) {
                        removedStoreFound = true;
                        break;
                    }
                }

                assertTrue("Store should not be in remote config cache", !removedStoreFound);
            }
        });
    }
}
