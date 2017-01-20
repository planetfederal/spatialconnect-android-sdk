/**
 * Copyright 2016 Boundless, http://boundlessgeo.com
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
package com.boundlessgeo.spatialconnect.mqtt;

import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.R;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCRemoteConfig;
import com.boundlessgeo.spatialconnect.schema.SCMessageOuterClass;
import com.boundlessgeo.spatialconnect.scutilities.SCTuple;
import com.boundlessgeo.spatialconnect.services.SCAuthService;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import rx.Observable;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

/**
 * MqttHandler is used to interact with the {@link org.eclipse.paho.android.service.MqttService} which acts as
 * the MQTT client, sending messages to and receiving messages from the broker.  This class handles authenticating
 * with the broker and other standard MQTT client operations (subscribe, publish, disconnect, etc).
 *
 * http://www.hivemq.com/blog/mqtt-client-library-enyclopedia-paho-android-service
 */
public class MqttHandler implements MqttCallbackExtended {

    private final static String LOG_TAG = MqttHandler.class.getSimpleName();

    private static MqttHandler instance;
    private MqttAndroidClient client;
    private Context context;
    private PublishSubject<SCTuple> scMessageSubject;
    public final static String REPLY_TO_TOPIC = String.format("/device/%s-replyTo", SpatialConnect.getInstance().getDeviceIdentifier());
    public static BehaviorSubject<Boolean> clientConnected = BehaviorSubject.create(false);
    private boolean isSecure;
    private Observable<SCTuple> multicast;

    private MqttHandler(Context context) {
        this.context = context;
        scMessageSubject = PublishSubject.create();
        multicast = scMessageSubject.share();
    }

    public static MqttHandler getInstance(Context context) {
        if (instance == null) {
            instance = new MqttHandler(context);
        }
        return instance;
    }

    /**
     * Initialize the MqttHander by creating an instance of MqttAndroidClient and registering this instance as the
     * client's callback listener.
     */
    public void initialize(SCRemoteConfig config) {
        Log.d(LOG_TAG, "initializing MqttHander.");
        if (config.getMqttProtocol().equalsIgnoreCase("ssl")) {
            isSecure = true;
        } else {
            isSecure = false;
        }
        String brokerUri = getMqttBrokerUri(config);
        client = new MqttAndroidClient(context, brokerUri, SpatialConnect.getInstance().getDeviceIdentifier());
        client.setCallback(this);
    }

    /**
     * Reads the {@code SCRemoteConfig} and returns the formatted mqtt broker uri.  The format is
     * "protocol://brokerHost:brokerPort" where protocol is tcp or ssl.
     *
     * @param config
     * @return the mqtt broker uri
     */
    private String getMqttBrokerUri(SCRemoteConfig config) {
        return String.format("%s://%s:%s",
                config.getMqttProtocol(),
                config.getMqttHost(),
                config.getMqttPort().toString()
        );
    }

    /**
     * Connect client to the MQTT broker with authToken as the username.
     */
    public void connect() {

        Log.d(LOG_TAG, "connecting to mqtt broker at " + client.getServerURI());
        // only try to connect to mqtt broker after the user has successfully authenticated
        final SCAuthService authService = SpatialConnect.getInstance().getAuthService();
        authService.getLoginStatus().subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer status) {
                if (status == SCAuthService.SCAuthStatus.AUTHENTICATED.value()) {
                    String accessToken = authService.getAccessToken();
                    try {
                        // set the clean session to remove any previous connection the broker may have for this client
                        MqttConnectOptions options = new MqttConnectOptions();
                        options.setCleanSession(false);
                        options.setAutomaticReconnect(true);
                        options.setUserName(accessToken);
                        options.setPassword("anypass".toCharArray());
                        if (isSecure) {
                            options.setSocketFactory(
                                    new SCSocketFactory(context.getResources().openRawResource(R.raw.ca))
                            );
                        }
                        client.connect(options, null, new ConnectActionListener());
                    }
                    catch (MqttException e) {
                        Log.e(LOG_TAG, "could not connect to mqtt broker.", e.getCause());
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "could not connect to mqtt broker.", e.getCause());
                    } catch (CertificateException e) {
                        Log.e(LOG_TAG, "could not connect to mqtt broker.", e.getCause());
                    } catch (NoSuchAlgorithmException e) {
                        Log.e(LOG_TAG, "could not connect to mqtt broker.", e.getCause());
                    } catch (KeyStoreException e) {
                        Log.e(LOG_TAG, "could not connect to mqtt broker.", e.getCause());
                    } catch (KeyManagementException e) {
                        Log.e(LOG_TAG, "could not connect to mqtt broker.", e.getCause());
                    }
                }
            }
        });
    }

    /**
     * Subscribe client to a topic.
     *
     * @param topic to subscribe to
     * @param qos   quality of service (0, 1, 2)
     */
    public void subscribe(final String topic, final int qos) {
        clientConnected.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean connected) {
                if (connected) {
                    try {
                        Log.d(LOG_TAG, "subscribing to topic " + topic + " with qos " + qos);
                        client.subscribe(topic, qos);
                    }
                    catch (MqttException e) {
                        Log.e(LOG_TAG, "could not subscribe to topic " + topic, e.getCause());
                    }
                }
            }
        });
    }

    /**
     * Publish message to a topic.
     *
     * @param topic   topic to publish the message to
     * @param message SCMessage to send as payload
     * @param qos     quality of service (0, 1, 2)
     */
    public void publish(final String topic, final SCMessageOuterClass.SCMessage message, final int qos) {
        clientConnected.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean connected) {
                if (connected) {
                    Log.d(LOG_TAG, "publishing to topic " + topic + " with qos " + qos);
                    // create a new MqttMessage from the message string
                    MqttMessage mqttMsg = new MqttMessage(message.toByteArray());
                    mqttMsg.setQos(qos);
                    try {
                        client.publish(topic, mqttMsg);
                    }
                    catch (MqttException e) {
                        Log.e(LOG_TAG, "could not publish to topic " + topic, e.getCause());
                    }
                }
            }
        });
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d(LOG_TAG, "Lost connection to mqtt broker.", cause);
        clientConnected.onNext(false);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.d(LOG_TAG, "received message on topic " + topic);
        SCMessageOuterClass.SCMessage scMessage = SCMessageOuterClass.SCMessage.parseFrom(message.getPayload());
        Log.d(LOG_TAG, "message payload: " + scMessage.getPayload());
        SCTuple scTuple = new SCTuple(topic, scMessage);
        scMessageSubject.onNext(scTuple);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // todo: update or remove the audit table rows when changes have been successfully delivered to backend
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            clientConnected.onNext(true);
            //clean session, re-subscribe
            SpatialConnect.getInstance().getBackendService().reconnect();
        }
    }

    public Observable<SCTuple> getMulticast() {
        return multicast;
    }

    /**
     * An implementation of an IMqttActionListener for connecting/authenticating to the broker.
     */
    class ConnectActionListener implements IMqttActionListener {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.d(LOG_TAG, "Connection Success!");
            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
            disconnectedBufferOptions.setBufferEnabled(true);
            disconnectedBufferOptions.setBufferSize(100);
            disconnectedBufferOptions.setPersistBuffer(false);
            disconnectedBufferOptions.setDeleteOldestMessages(true);
            client.setBufferOpts(disconnectedBufferOptions);
            clientConnected.onNext(true);
            scMessageSubject.publish();
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable ex) {
            Log.d(LOG_TAG, "Connection Failure!", ex);
            clientConnected.onNext(false);
        }
    }
}
