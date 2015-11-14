package com.boundlessgeo.spatialconnect.test;

import android.location.Location;

import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;
import com.boundlessgeo.spatialconnect.services.SCServiceStatus;

import rx.observers.TestSubscriber;


public class SensorServiceTest extends BaseTestCase {

    public void testSensorServiceStarts() {
        SCServiceManager serviceManager = new SCServiceManager(testContext);
        serviceManager.startAllServices();
        assertTrue("The sensor service should have started",
                serviceManager.getSensorService().getStatus().equals(SCServiceStatus.SC_SERVICE_RUNNING)
        );
    }

    public void testSCSensorService() {
        SCSensorService sensorService = new SCSensorService(testContext);
        sensorService.startGPSListener();
        assertTrue("The GPS listener should have started.", sensorService.gpsListenerStarted());
        TestSubscriber<Location> testSubscriber = new TestSubscriber<>();
        sensorService.getLastKnownLocation().subscribe(testSubscriber);
        testSubscriber.assertNotCompleted();
        sensorService.disableGPSListener();
        assertTrue("The GPS listener should have stopped.", !sensorService.gpsListenerStarted());
    }

}
