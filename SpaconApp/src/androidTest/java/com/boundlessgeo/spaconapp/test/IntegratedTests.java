package com.boundlessgeo.spaconapp.test;

import android.app.Activity;
import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.boundlessgeo.spaconapp.MainActivity;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCConfig;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.mqtt.MqttHandler;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.schema.SCMessageOuterClass;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.scutilities.Json.JsonUtilities;
import com.boundlessgeo.spatialconnect.scutilities.Json.SCObjectMapper;
import com.boundlessgeo.spatialconnect.services.SCBackendService;
import com.boundlessgeo.spatialconnect.stores.FormStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.WFSStore;
import com.fasterxml.jackson.databind.JsonNode;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
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
    private static String TOKEN;

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

    @Ignore
    public void testWFS_1_StoreCorrectly() {
        boolean containsWFSStore = false;
        for (SCDataStore store : sc.getDataService().getActiveStores()) {
            if (store.getType().equals(WFSStore.TYPE)) containsWFSStore = true;
        }
        assertTrue("A wfs store should be running.", containsWFSStore);
        SCDataStore wfsStore = sc.getDataService().getStoreByIdentifier(WFS_STORE_ID);
        assertEquals("The wfs store should be running", SCDataStoreStatus.SC_DATA_STORE_RUNNING, wfsStore.getStatus());
    }

    @Ignore
    public void testWFS_2_GetCapabilitesUrlIsBuiltCorrectly() {
        WFSStore store = (WFSStore) sc.getDataService().getStoreByIdentifier(WFS_STORE_ID);
        assertEquals("The WFS store url was not built correctly.",
                "http://demo.boundlessgeo.com/geoserver/osm/ows?service=WFS&version=1.1.0&request=GetCapabilities",
                store.getGetCapabilitiesUrl()
        );
    }

    @Ignore
    public void testWFS_3_LayerNamesAreParsedFromCapabilities() {
        WFSStore store = (WFSStore) sc.getDataService().getStoreByIdentifier(WFS_STORE_ID);
        assertTrue("There should be multiple layers.",
                store.layers().size() > 0
        );
    }

    @Ignore
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

    @Test
    public void testZ_FormSubmission() {
        sc.getBackendService()
                .configReceived
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
                    public void onNext(Boolean received) {
                        if (received) {
                            startFormSubmission();
                        }
                    }
                });
    }

    private void startFormSubmission() {
        try {
            // build feature to submit using data from response
            String formId = getValidFormId();
            String sample = getSampleFormSubmission(formId);
            String sampleFeatureJson = SCObjectMapper.getMapper().readTree(sample).at("/result/feature").toString();
            SCSpatialFeature feature = new JsonUtilities().getSpatialDataTypeFromJson(sampleFeatureJson);
            String tableName = SCObjectMapper.getMapper().readTree(sample).at("/result/form_key").asText();
            feature.setLayerId(tableName);
            FormStore formStore = SpatialConnect.getInstance().getDataService().getFormStore();
            TestSubscriber formSubscriber = new TestSubscriber();
            formStore.create(feature).subscribe(formSubscriber);
            formSubscriber.awaitTerminalEvent();
            formSubscriber.assertNoErrors();
            SCSpatialFeature formSubmission = (SCSpatialFeature) formSubscriber.getOnNextEvents().get(0);
            assertTrue(formSubmission != null);
            Thread.sleep(1000);
            assertThatFormSubmissionWasWrittenToServer(formId, formSubmission);
        } catch (IOException e) {
            assertTrue("This exception should not be thrown.", false);
        } catch (InterruptedException e) {
            assertTrue("This exception should not be thrown.", false);
        }
    }

    private void assertThatFormSubmissionWasWrittenToServer(String formId, SCSpatialFeature submission)
            throws IOException {
        TestSubscriber testSubscriber = new TestSubscriber();
        String formResultsUrl = getApiUrl() + "/api/form/" + formId + "/results";
        HttpHandler.getInstance().get(formResultsUrl, TOKEN).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        Response response = (Response) testSubscriber.getOnNextEvents().get(0);
        JsonNode root = SCObjectMapper.getMapper().readTree(response.body().string());
        boolean submissionFound = false;
        Iterator<JsonNode> submissions = root.get("result").iterator();
        while(submissions.hasNext()) {
            JsonNode node = submissions.next();
            // first check that this submission was made by this device
            String client = node.at("/val/metadata/client").asText();
            if (!client.equals(SpatialConnect.getInstance().getDeviceIdentifier())) {
                continue;
            }
            // now we can check the id of the submitted feature, which is unique to this device
            String submissionId = node.at("/val/id").asText();
            if (submissionId.equals(submission.getId())) {
                submissionFound = true;
            }
        }
        assertTrue("The response should contain the id of the form submission", submissionFound);
    }

    private String getSampleFormSubmission(String formId) throws IOException {
        TestSubscriber testSubscriber = new TestSubscriber();
        String formSampleUrl = getApiUrl() + "/api/form/" + formId + "/sample"; //todo: s/form/forms
        HttpHandler.getInstance().get(formSampleUrl, TOKEN).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        Response response = (Response) testSubscriber.getOnNextEvents().get(0);
        return response.body().string();
    }

    private String getValidFormId() throws IOException {
        String formsUrl = getApiUrl() + "/api/forms";
        TestSubscriber testSubscriber = new TestSubscriber();
        HttpHandler.getInstance().get(formsUrl, TOKEN).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        Response response = (Response) testSubscriber.getOnNextEvents().get(0);
        JsonNode root = SCObjectMapper.getMapper().readTree(response.body().string());
        return root.at("/result/0/id").asText();
    }


    private String getApiUrl() throws IOException {
        SCConfig scConfig = SCObjectMapper.getMapper().readValue(remoteConfigFile, SCConfig.class);
        return scConfig.getRemote().getHttpUri();
    }

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(2, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }
}
