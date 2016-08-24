package com.boundlessgeo.spatialconnect.test;

import com.boundlessgeo.spatialconnect.SpatialConnect;

import org.junit.BeforeClass;

public class MqttHandlerTest extends BaseTestCase {

    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(testConfigFile);
        sc.startAllServices();
        sc.getAuthService().authenticate("admin@something.com", "admin");
    }

//    @Test
//    public void testMqttHanderCanConnectToBroker() {
//        MqttHandler mqttHandler = MqttHandler.getInstance(activity);
//        assertTrue("The MqttHander should have connected to the broker after authenicating.",
//                mqttHandler.isConnected());
//    }


}
