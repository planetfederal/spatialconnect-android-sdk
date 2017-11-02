/**
 * Copyright 2015-2017 Boundless, http://boundlessgeo.com
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
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.services;


import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.net.NetworkInfo;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.geometries.SCPoint;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.scutilities.LocationHelper;
import com.boundlessgeo.spatialconnect.scutilities.SCCache;
import com.boundlessgeo.spatialconnect.stores.LocationStore;
import com.github.pwittchen.reactivenetwork.library.Connectivity;
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

/**
 * The SCSensorService provides access to various sensors inputs that can be captured by the mobile device.  This
 * includes the GPS, the camera, the microphone, and various Bluetooth connections.
 */
public class SCSensorService extends SCService implements SCServiceLifecycle{

    private static final String SERVICE_NAME = "SC_SENSOR_SERVICE";
    private static final String GPS_ENABLED = "service.sensor.gps.enabled";
    private Context context;
    private LocationHelper locationHelper;
    private boolean gpsListenerStarted;
    private final String LOG_TAG = SCSensorService.class.getSimpleName();
    private BehaviorSubject<Boolean> isConnected = BehaviorSubject.create(false);
    private PublishSubject<Boolean> isEnabled = PublishSubject.create();
    private int accuracy;
    private float distance;
    private Observable<SCPoint> lastKnown;

    public SCSensorService(Context context) {
        this.context = context;
        accuracy = Criteria.ACCURACY_FINE;
        distance = 1000; // every second
        locationHelper = new LocationHelper(context);
    }

    @Override
    public boolean start(Map<String, SCService> deps) {
        boolean started = super.start(deps);
        setupObservables();
        return started;
    }

    @Override
    public String getId() {
        return SERVICE_NAME;
    }

    @Override
    public List<String> getRequires() {
        return null;
    }

    /**
     * Behavior subject return YES for Internet access, NO for offline
     * @return BehaviorSubject<Boolean>
     */
    public BehaviorSubject<Boolean> isConnected() {
        return isConnected;
    }

    public PublishSubject<Boolean> isEnabled() {
        return isEnabled;
    }

    /**
     * Sets location accuracy for location updates
     * @param accuracy Criteria.ACCURACY_COARSE, Criteria.ACCURACY_FINE, Criteria.ACCURACY_HIGH
     *                 Criteria.ACCURACY_LOW
     * @param distance
     */
    public void setLocationAccuracy(int accuracy, float distance) {
        this.accuracy = accuracy;
        this.distance = distance;
    }

    /**
     * Turns on location updates
     */
    public void enableGPS() {
        SpatialConnect sc = SpatialConnect.getInstance();
        SCCache cache = sc.getCache();
        cache.setValue(true, GPS_ENABLED);
        isEnabled.onNext(true);

        if (locationHelper.isGPSPermissionGranted()) {
            if (!gpsListenerStarted) {
                locationHelper.enableGps(accuracy, distance);
                gpsListenerStarted = true;
                return;
            }
            else {
              Log.w(LOG_TAG, "Attempted to start GPS listener but was unsuccessful.");
            }
        } else {
            Log.i(LOG_TAG, "GPS permission has not been granted");
        }

        final LocationStore locationStore = sc.getDataService().getLocationStore();
        lastKnown.flatMap(new Func1<SCPoint, Observable<SCSpatialFeature>>() {
            @Override
            public Observable<SCSpatialFeature> call(SCPoint scPoint) {
                return locationStore.create(scPoint);
            }
        });
    }

    /**
     * Turns off location updates
     */
    public void disableGPS() {
        locationHelper.disableGps();
        gpsListenerStarted = false;

        SpatialConnect sc = SpatialConnect.getInstance();
        SCCache cache = sc.getCache();
        cache.setValue(false, GPS_ENABLED);

        isEnabled.onNext(false);
    }

    /**
     * Last known location of the device emiting {@link Location} over an Observable
     *
     * @return Observable stream of Location
     */
    public Observable<Location> getLastKnownLocation() {
        return locationHelper.getLocation();
    }

    /**
     * Last known location of the device emitting {@link SCPoint} over an Observable
     * @return Observable stream of Lo{@link SCPoint}cation
     */
    public Observable<SCPoint> getLastKnown() {
        return lastKnown;
    }

    /**
     * Helper method to determine if SCSensorService has enabled the gps.
     *
     * @return  true if gpsListener has been started, false otherwise.
     */
    public boolean gpsListenerStarted() {
        return this.gpsListenerStarted;
    }

    private void setupObservables() {
        lastKnown = locationHelper.getLocation()
                .map(new Func1<Location, SCPoint>() {
                    @Override
                    public SCPoint call(Location location) {
                        Coordinate coordinate = new Coordinate(location.getLongitude(),location.getLatitude(),location.getAltitude());
                        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 0);
                        Geometry geometry = geometryFactory.createPoint(coordinate);
                        return new SCPoint(geometry);
                    }
                });

        ReactiveNetwork.observeNetworkConnectivity(context)
                .subscribe(new Action1<Connectivity>() {
                    @Override
                    public void call(Connectivity connectivity) {
                        Log.d(LOG_TAG, "Connectivity is " + connectivity.getState().name());
                        isConnected.onNext(connectivity.getState().equals(NetworkInfo.State.CONNECTED));
                    }
                });
    }

    public static String serviceId() {
        return SERVICE_NAME;
    }
}
