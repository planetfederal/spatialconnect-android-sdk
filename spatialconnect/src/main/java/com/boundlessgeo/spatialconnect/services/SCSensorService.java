package com.boundlessgeo.spatialconnect.services;


import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.boundlessgeo.spatialconnect.scutilities.LocationHelper;

import rx.Observable;
import rx.functions.Action1;

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
            if (!gpsListenerStarted) {
                locationHelper.enableGps();
                gpsListenerStarted = true;
                return;
            }
            else {
              Log.w(LOG_TAG, "Attempted to start GPS listener but was unsuccessful.");
            }
        } else {
            // explicitly ask for permission
            locationHelper.requestGPSPermission().subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean permissionGranted) {
                    if (permissionGranted) {
                        locationHelper.enableGps();
                        gpsListenerStarted = true;
                    } else {
                        Log.w(LOG_TAG, "Cannot start GPS listener b/c permission was denied.");
                    }
                }
            });
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

}
