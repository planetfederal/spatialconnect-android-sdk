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

import rx.observers.TestSubscriber;

public class LocationHelperTest extends BaseTestCase {

    private static LocationManager locationManager;
    private static final String TEST_PROVIDER_NAME = LocationManager.GPS_PROVIDER;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // add a new mock provider
        locationManager = (LocationManager) this.testContext.getSystemService(Context.LOCATION_SERVICE);
        locationManager.addTestProvider(TEST_PROVIDER_NAME, false, false, false, false, false, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled(TEST_PROVIDER_NAME, true);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
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
    public void testLocationUpdates() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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
