package com.boundlessgeo.spatialconnect.cloudMessaging;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CloudMessagingService extends FirebaseMessagingService {

    private static final String LOG_TAG = CloudMessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(LOG_TAG,"onMessageReceived");
    }
}
