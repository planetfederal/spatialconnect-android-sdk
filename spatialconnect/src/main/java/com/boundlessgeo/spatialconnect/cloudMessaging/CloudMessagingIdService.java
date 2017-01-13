package com.boundlessgeo.spatialconnect.cloudMessaging;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class CloudMessagingIdService extends FirebaseInstanceIdService {

    private static final String LOG_TAG = CloudMessagingIdService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        sendRegistrationTokenToServer(refreshedToken);
    }

    private void sendRegistrationTokenToServer(String token) {
        Log.d(LOG_TAG, "cloud token: " + token);
        // TODO: Implement this method to send token mqtt.
    }
}
