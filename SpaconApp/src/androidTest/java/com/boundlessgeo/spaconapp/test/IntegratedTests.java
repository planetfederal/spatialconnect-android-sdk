package com.boundlessgeo.spaconapp.test;

import android.app.Activity;
import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.boundlessgeo.spaconapp.MainActivity;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.mqtt.MqttHandler;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.schema.SCMessageOuterClass;
import com.boundlessgeo.spatialconnect.services.SCBackendService;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.WFSStore;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IntegratedTests {

    private static String LOG_TAG = IntegratedTests.class.getSimpleName();
    private static final String WFS_STORE_ID = "280d72a9-eb45-4ba1-a5b4-c7f5bc2dd7fe";
    protected static Activity activity = null;
    protected static Context testContext = null;

    /**
     * Test context
     */
    protected static File remoteConfigFile;

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
            sc.getConfigService().addConfigFilePath(remoteConfigFile.getAbsolutePath());
            sc.startAllServices();
            sc.getAuthService().authenticate("admin@something.com", "admin");
            waitForStoreToStart(WFS_STORE_ID);

        } catch (IOException ex) {
            System.exit(0);
        }
    }

    @After
    public void afterTest() throws Exception {
    }

    @Test
    public void testMqttNetworkServiceCanPublishAndSubscribe() {
        SCBackendService networkService = sc.getBackendService();
        SCMessageOuterClass.SCMessage.Builder builder =  SCMessageOuterClass.SCMessage.newBuilder();
        builder.setAction(0)
                .setPayload("{\"testing\": true}")
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
        assertEquals("The reply message payload should be {\"result\":\"pong\"}",
                "{\"result\":\"pong\"}",
                testSubscriber.getOnNextEvents().get(0).getPayload());
    }

    @Test
    public void testMqttLoadLocalConfigs() {
        sc.getDataService()
                .hasStores
                .buffer(2)
                .subscribe(new Action1<List<Boolean>>() {
                    @Override
                    public void call(List<Boolean> booleen) {
                        assertTrue("The remote config file has at least 2 stores",
                                sc.getDataService().getStoreList().size() >= 2);
                    }
        });
    }

    @Test
    public void testMqttConnected() {
        SpatialConnect sc = SpatialConnect.getInstance();
        sc.getBackendService()
                .connectedToBroker
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, "onError()\n" + e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(Boolean connected) {
                        assertTrue("Mqtt should be connected",
                                connected);
                    }
                });
    }

    @Test
    public void testWFS_1_StoreCorrectly() {
        boolean containsWFSStore = false;
        for (SCDataStore store : sc.getDataService().getActiveStores()) {
            if (store.getType().equals(WFSStore.TYPE)) containsWFSStore = true;
        }
        assertTrue("A wfs store should be running.", containsWFSStore);
        SCDataStore wfsStore = sc.getDataService().getStoreByIdentifier(WFS_STORE_ID);
        assertEquals("The wfs store should be running", SCDataStoreStatus.SC_DATA_STORE_RUNNING, wfsStore.getStatus());
    }

    @Test
    public void testWFS_2_GetCapabilitesUrlIsBuiltCorrectly() {
        WFSStore store = (WFSStore) sc.getDataService().getStoreByIdentifier(WFS_STORE_ID);
        assertEquals("The WFS store url was not built correctly.",
                "http://demo.boundlessgeo.com/geoserver/osm/ows?service=WFS&version=1.1.0&request=GetCapabilities",
                store.getGetCapabilitiesUrl()
        );
    }

    @Test
    public void testWFS_3_LayerNamesAreParsedFromCapabilities() {
        WFSStore store = (WFSStore) sc.getDataService().getStoreByIdentifier(WFS_STORE_ID);
        assertTrue("There should be multiple layers.",
                store.layers().size() > 0
        );
    }

    @Test
    public void testWFS_4_QueryWithBbox() {
        SCBoundingBox bbox = new SCBoundingBox(-127.056432967933, 42.03578985948257, -52.696780484684545, 62.464526783166164);
        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
        );
        WFSStore store = (WFSStore) sc.getDataService().getStoreByIdentifier(WFS_STORE_ID);
        filter.addLayerId("buildings");
        TestSubscriber testSubscriber = new TestSubscriber();
        store.query(filter).timeout(3, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent(); // it never completes, only times out
        assertTrue("The query observable should have emitted features", testSubscriber.getOnNextEvents().size() > 0);
        // TODO: test that the onNext events are valid scSpatialFeatures
    }

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(2, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }
}
