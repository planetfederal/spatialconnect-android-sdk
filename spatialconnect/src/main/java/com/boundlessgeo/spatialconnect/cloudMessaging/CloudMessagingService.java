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
package com.boundlessgeo.spatialconnect.cloudMessaging;

import android.util.Log;

import com.boundlessgeo.schema.MessagePbf;
import com.boundlessgeo.spatialconnect.mqtt.SCNotification;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import rx.Observable;
import rx.subjects.PublishSubject;

public class CloudMessagingService extends FirebaseMessagingService {

    private static final String LOG_TAG = CloudMessagingService.class.getSimpleName();
    private static PublishSubject<SCNotification> firebaseMessageSubject = PublishSubject.create();
    private static Observable<SCNotification> multicast = firebaseMessageSubject.share();

    public static Observable<SCNotification> getMulticast() {
        return multicast;
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(LOG_TAG,"onMessageReceived (FCM) payload: " + remoteMessage.getData().get("payload"));
        MessagePbf.Msg cloudMessage = MessagePbf.Msg.newBuilder()
                .setPayload(remoteMessage.getData().get("payload"))
                .build();
        SCNotification notification = new SCNotification(cloudMessage);
        firebaseMessageSubject.onNext(notification);
    }
}
