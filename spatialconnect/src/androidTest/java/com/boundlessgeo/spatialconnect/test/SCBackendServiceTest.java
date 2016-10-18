package com.boundlessgeo.spatialconnect.test;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.mqtt.MqttHandler;
import com.boundlessgeo.spatialconnect.schema.SCMessageOuterClass;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.services.SCAuthService;
import com.boundlessgeo.spatialconnect.services.SCBackendService;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class SCBackendServiceTest extends BaseTestCase {

    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(remoteConfigFile);
        sc.startAllServices();
        sc.getAuthService().authenticate("admin@something.com", "admin");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
//        deleteDatabases();
    }

    @Test
    public void testAuthCredentialsAreSaved() {
        SCAuthService.loginStatus.filter(new Func1<Boolean, Boolean>() {
            @Override public Boolean call(Boolean status) {
                return status;
            }
        }).subscribe(new Action1<Boolean>() {
            @Override public void call(Boolean aBoolean) {
                assertTrue("Email should be saved in preferences.",
                    SCAuthService.getEmail().equals("admin@something.com"));
                assertTrue("Password should be saved in preferences.",
                    SCAuthService.getPassword().equals("admin"));
                assertTrue("Access token should be saved in preferences.",
                    SCAuthService.getAccessToken() != null);
            }
        });
    }

    @Test
    @Ignore
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
}
