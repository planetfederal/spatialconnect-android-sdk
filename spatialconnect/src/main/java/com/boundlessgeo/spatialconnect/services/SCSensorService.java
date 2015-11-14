package com.boundlessgeo.spatialconnect.services;


import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.boundlessgeo.spatialconnect.scutilities.LocationHelper;

import rx.Observable;

/**
 * The SCSensorService provides access to various sensors inputs that can be captured by the mobile device.  This
 * includes the GPS, the camera, the microphone, and various Bluetooth connections.
 */
public class SCSensorService extends SCService {

    private Context context;
    private LocationHelper locationHelper;
    private boolean gpsListenerStarted;
    private final String LOG_TAG = SCSensorService.class.getSimpleName();


    public SCSensorService(Context context) {
        this.context = context;
        locationHelper = new LocationHelper(context);
    }

    /**
     * Calls the SCService#start() method to set the status to SC_SERVICE_RUNNING
     */
    public void start() {
        super.start();
    }

    /**
     * Initializes the LocationHelper to start listening to updates by the GPS LocationProvider.
     */
    public void startGPSListener() {
        if (locationHelper.isGPSPermissionGranted()) {
            if (!gpsListenerStarted()) {
                locationHelper.enableGps();
                gpsListenerStarted = true;
            }
        }
        Log.w(LOG_TAG, "Attempted to start GPS listener but was unsuccessful.");
    }

    /**
     * Disables the GPS listener by unsubscribing it from the GPS LocationProvider.
     */
    public void disableGPSListener() {
        locationHelper.disableGps();
        gpsListenerStarted = false;
    }

    /**
     * Ensures the GPS location listener has started and then returns an Observable sequence of Location instances.
     *
     * @return Observable stream of Location or null
     */
    public Observable<Location> getLastKnownLocation() {
        if (!gpsListenerStarted) {
            if (!gpsListenerStarted()) {
                return null;
            }
        }
        return locationHelper.getLocation();
    }

    public boolean gpsListenerStarted() {
        return this.gpsListenerStarted;
    }
}
