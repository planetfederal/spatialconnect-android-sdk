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
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.services;


import android.content.Context;
import android.location.Location;
import android.net.NetworkInfo;
import android.util.Log;

import com.boundlessgeo.spatialconnect.scutilities.LocationHelper;
import com.github.pwittchen.reactivenetwork.library.Connectivity;
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork;

import rx.Observable;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

/**
 * The SCSensorService provides access to various sensors inputs that can be captured by the mobile device.  This
 * includes the GPS, the camera, the microphone, and various Bluetooth connections.
 */
public class SCSensorService extends SCService implements SCServiceLifecycle{

    private static final String SERVICE_NAME = "SC_SENSOR_SERVICE";
    private Context context;
    private LocationHelper locationHelper;
    private boolean gpsListenerStarted;
    private final String LOG_TAG = SCSensorService.class.getSimpleName();
    public static BehaviorSubject<Integer> running = BehaviorSubject.create(0);
    public BehaviorSubject<Boolean> isConnected = BehaviorSubject.create(false);


    public SCSensorService(Context context) {
        this.context = context;
        locationHelper = new LocationHelper(context);
    }

    /**
     * Calls the SCService#start() method to set the status to SC_SERVICE_RUNNING
     */
    @Override
    public Observable<Void> start() {
        super.start();
        running.onNext(1);
        ReactiveNetwork.observeNetworkConnectivity(context)
                .subscribe(new Action1<Connectivity>() {
                    @Override
                    public void call(Connectivity connectivity) {
                        Log.d(LOG_TAG, "Connectivity is " + connectivity.getState().name());
                        isConnected.onNext(connectivity.getState().equals(NetworkInfo.State.CONNECTED));
                    }
                });

        return Observable.empty();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void startError() {
        super.startError();
    }

    /**
     * Initializes the LocationHelper to start listening to updates by the GPS LocationProvider.
     */
    public void startGPSListener() {
        if (locationHelper.isGPSPermissionGranted()) {
            if (!gpsListenerStarted) {
                locationHelper.enableGps();
                gpsListenerStarted = true;
                return;
            }
            else {
              Log.w(LOG_TAG, "Attempted to start GPS listener but was unsuccessful.");
            }
        } else {
            Log.i(LOG_TAG, "GPS permission has not been granted");
        }
    }

    /**
     * Disables the GPS listener by unsubscribing it from the GPS LocationProvider.
     */
    public void disableGPSListener() {
        locationHelper.disableGps();
        gpsListenerStarted = false;
    }

    /**
     * Returns an Observable sequence of Location instances.
     *
     * @return Observable stream of Location
     */
    public Observable<Location> getLastKnownLocation() {
        return locationHelper.getLocation();
    }

    /**
     * Helper method to determine if SCSensorService has enabled the gps.
     *
     * @return  true if gpsListener has been started, false otherwise.
     */
    public boolean gpsListenerStarted() {
        return this.gpsListenerStarted;
    }

    public static String serviceId() {
        return SERVICE_NAME;
    }
}
