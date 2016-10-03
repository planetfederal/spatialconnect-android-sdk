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

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.test.suitebuilder.annotation.Suppress;
import com.boundlessgeo.spatialconnect.scutilities.LocationHelper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

public class LocationHelperTest extends BaseTestCase {

  private static final String TEST_PROVIDER_NAME = LocationManager.GPS_PROVIDER;
  private static LocationManager locationManager;

  @Before public void setUp() throws Exception {
    // add a new mock provider
    locationManager = (LocationManager) this.testContext.getSystemService(Context.LOCATION_SERVICE);
    locationManager.addTestProvider(TEST_PROVIDER_NAME, false, false, false, false, false, true,
        true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
    locationManager.setTestProviderEnabled(TEST_PROVIDER_NAME, true);
  }

  @After public void tearDown() throws Exception {
    locationManager.clearTestProviderEnabled(TEST_PROVIDER_NAME);
    locationManager.removeTestProvider(TEST_PROVIDER_NAME);
  }

  /**
   * This method requires that the phone/emulator allows mock GPS positions to be set.
   *
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   */
  @Suppress // suppressing b/c gps testing should be tested on an actual device
  @Test public void testLocationUpdates()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    TestSubscriber<Location> testSubscriber = new TestSubscriber<>();

    // setup LocationHelper
    LocationHelper locationHelper = new LocationHelper(super.testContext);

    // create a new location
    Location mockLocation = new Location(TEST_PROVIDER_NAME);
    mockLocation.setLatitude(29.9);
    mockLocation.setLongitude(-90.0);
    mockLocation.setTime(System.currentTimeMillis());

    // hack for jelly bean: http://jgrasstechtips.blogspot.com/2012/12/android-incomplete-location-object.html
    Method locationJellyBeanFixMethod = Location.class.getMethod("makeComplete");
    if (locationJellyBeanFixMethod != null) {
      locationJellyBeanFixMethod.invoke(mockLocation);
    }

    locationHelper.enableGps();

    locationHelper.getLocation().subscribe(testSubscriber);

    //        LocationListener nonRxListener = new LocationListener() {
    //            public void onLocationChanged(Location location) {
    //                System.out.println("nonRx listener location is" + location.toString());
    //            }
    //            @Override
    //            public void onStatusChanged(String provider, int status, Bundle extras) {
    //            }
    //
    //            @Override
    //            public void onProviderEnabled(String provider) {
    //            }
    //
    //            @Override
    //            public void onProviderDisabled(String provider) {
    //            }
    //        };
    //
    //        locationManager.requestLocationUpdates(TEST_PROVIDER_NAME, 1000, 1, nonRxListener);

    // simulate GPS location update using mock location and test provider
    locationManager.setTestProviderLocation(TEST_PROVIDER_NAME, mockLocation);

    testSubscriber.assertNoErrors();
    testSubscriber.assertNotCompleted();
    testSubscriber.assertReceivedOnNext(Arrays.asList(mockLocation));

    locationHelper.disableGps();
  }
}
