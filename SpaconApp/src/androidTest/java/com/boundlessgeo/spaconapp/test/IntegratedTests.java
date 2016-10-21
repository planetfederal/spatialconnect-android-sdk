package com.boundlessgeo.spaconapp.test;

import android.app.Activity;
import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.boundlessgeo.spaconapp.MainActivity;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.SpatialConnectActivity;
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
            InputStream is = testContext.getResources().openRawResource(R.raw.scconfig_remote);
            FileOutputStream fos = new FileOutputStream(remoteConfigFile);
            byte[] data = new byte[is.available()];
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
    public void testNewtworkServiceCanPublishAndSubscribe() {
        SCBackendService networkService = sc.getBackendService();
        SCMessageOuterClass.SCMessage.Builder builder =  SCMessageOuterClass.SCMessage.newBuilder();
        builder.setAction(0)
                .setPayload("testing")
                .setReplyTo(MqttHandler.REPLY_TO_TOPIC)
                .build();
        SCMessageOuterClass.SCMessage message = builder.build();
        TestSubscriber<SCMessageOuterClass.SCMessage> testSubscriber = new TestSubscriber();
        networkService.publishReplyTo("/ping", message)
                .take(1)
                .timeout(15, TimeUnit.SECONDS)
                .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(1);
        testSubscriber.assertCompleted();
        assertEquals("The reply message payload should be 'pong'.",
                "pong",
                testSubscriber.getOnNextEvents().get(0).getPayload());
    }

    @Test
    public void testSpatialConnectCanLoadLocalConfigs() {
        sc.getDataService()
                .hasStores
                .buffer(2)
                .subscribe(new Action1<List<Boolean>>() {
                    @Override
                    public void call(List<Boolean> booleen) {
                        assertEquals("The remote config file has at least 2 stores",
                                2, sc.getDataService().getAllStores().size());
                    }
        });
    }
}
