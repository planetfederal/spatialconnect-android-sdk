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

import android.location.Location;
import android.test.suitebuilder.annotation.Suppress;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCServiceStatus;
import org.junit.Test;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertTrue;

public class SensorServiceTest extends BaseTestCase {

  @Suppress // suppressing until there is an "all services started" event that we can subscribe to
  @Test public void testSensorServiceStarts() {
    SpatialConnect sc = SpatialConnect.getInstance();
    sc.initialize(activity);
    sc.startAllServices();
    assertTrue("The sensor service should have started",
        sc.getSensorService().getStatus().equals(SCServiceStatus.SC_SERVICE_RUNNING));
  }

  @Suppress // suppressing until there is an "all services started" event that we can subscribe to
  @Test public void testSCSensorService() {
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
