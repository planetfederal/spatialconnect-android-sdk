package com.boundlessgeo.spatialconnect.cloudMessaging;

import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.mqtt.SCNotification;
import com.boundlessgeo.spatialconnect.schema.SCMessageOuterClass;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CloudMessagingService extends FirebaseMessagingService {

    private static final String LOG_TAG = CloudMessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(LOG_TAG,"onMessageReceived (FCM) payload: " + remoteMessage.getData().get("payload"));
        SCMessageOuterClass.SCMessage cloudMessage = SCMessageOuterClass.SCMessage.newBuilder()
                .setPayload(remoteMessage.getData().get("payload"))
                .build();
        SCNotification notification = new SCNotification(cloudMessage);
        SpatialConnect sc = SpatialConnect.getInstance();
        sc.getBackendService().addNotification(notification);
    }
}
