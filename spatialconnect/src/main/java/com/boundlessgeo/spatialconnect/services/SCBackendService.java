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
package com.boundlessgeo.spatialconnect.services;


import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCConfig;
import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCRemoteConfig;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.mqtt.MqttHandler;
import com.boundlessgeo.spatialconnect.mqtt.QoS;
import com.boundlessgeo.spatialconnect.mqtt.SCNotification;
import com.boundlessgeo.spatialconnect.schema.SCCommand;
import com.boundlessgeo.spatialconnect.schema.SCMessageOuterClass;
import com.boundlessgeo.spatialconnect.scutilities.Json.JsonUtilities;
import com.boundlessgeo.spatialconnect.scutilities.Json.SCObjectMapper;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.google.protobuf.Timestamp;

import java.io.IOException;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

/**
 * SCBackendService handles any communication with backend SpatialConnect services.
 */
public class SCBackendService extends SCService implements SCServiceLifecycle {

    private static final String LOG_TAG = SCBackendService.class.getSimpleName();
    private static final String SERVICE_NAME = "SC_BACKEND_SERVICE";
    private static Context context;
    private static MqttHandler mqttHandler;
    public static BehaviorSubject<Boolean> configReceived = BehaviorSubject.create(false);
    public BehaviorSubject<Boolean> connectedToBroker = BehaviorSubject.create(false);
    public BehaviorSubject<SCNotification> notifications = BehaviorSubject.create();
    public static String backendUri = null;

    public SCBackendService(final Context context) {
        this.context = context;
        this.mqttHandler = MqttHandler.getInstance(context);
    }

    /**
     * Initialize the backend service with a {@code SCRemoteConfig} to setup connections to the SpatialConnect backend
     * including the REST API and the MQTT broker.
     *
     * @param config
     */
    public void initialize(SCRemoteConfig config) {
        if (backendUri == null) {
            backendUri = String.format(
                    "%s://%s:%s",
                    config.getHttpProtocol(),
                    config.getHttpHost(),
                    config.getHttpPort().toString()
            );
        }
        mqttHandler.initialize(config);
        setupSubscriptions();
        setupMqttConnectionListener();
    }

    @Override
    public Observable<SCServiceStatus> start() {
        super.start();
        mqttHandler.connect();
        return Observable.create(new Observable.OnSubscribe<SCServiceStatus>() {
             @Override
             public void call(final Subscriber<? super SCServiceStatus> subscriber) {

                 try {
                     subscriber.onNext(getStatus());
                     SCSensorService sensorService = SpatialConnect.getInstance().getSensorService();
                     sensorService.isConnected.subscribe(new Action1<Boolean>() {
                         @Override
                         public void call(Boolean connected) {
                             if (connected) {
                                 Log.d(LOG_TAG, "waiting on auth to get remote from server");
                                 SCAuthService.loginStatus.subscribe(new Action1<Boolean>() {
                                     @Override
                                     public void call(Boolean authenticated) {
                                         if (authenticated) {
                                             registerAndFetchConfig();
                                         } else {
                                             SpatialConnect.getInstance().getConfigService().loadConfigFromCache();
                                             configReceived.onNext(true);
                                         }
                                     }
                                 });
                             } else {
                                 //load config from cache
                                 Log.d(LOG_TAG, "No internet get cached remote config");
                                 SpatialConnect.getInstance().getConfigService().loadConfigFromCache();
                                 configReceived.onNext(true);

                             }
                             subscriber.onNext(SCServiceStatus.SC_SERVICE_RUNNING);
                            }
                        });
                 } catch (Exception e) {
                     subscriber.onError(e);
                 }
             }
         });
    }

    private void setupSubscriptions() {
        listenOnTopic("/notify")
                .subscribe(new Action1<SCMessageOuterClass.SCMessage>() {
                    @Override
                    public void call(SCMessageOuterClass.SCMessage scMessage) {
                        SCNotification notification = new SCNotification(scMessage);
                        notifications.onNext(notification);
                    }
                });
    }

    private void setupMqttConnectionListener() {
        MqttHandler.clientConnected.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean clientConnected) {
                connectedToBroker.onNext(clientConnected);
            }
        });
    }

    private void registerAndFetchConfig() {
        registerDevice();
        fetchConfig();
        listenForUpdates();
    }

    private void fetchConfig() {
        Log.d(LOG_TAG, "fetching config from mqtt config topic");
        SCMessageOuterClass.SCMessage getConfigMsg = SCMessageOuterClass.SCMessage.newBuilder()
                .setAction(SCCommand.CONFIG_FULL.value()).build();
        publishReplyTo("/config", getConfigMsg)
                .subscribe(new Action1<SCMessageOuterClass.SCMessage>() {
                    @Override
                    public void call(SCMessageOuterClass.SCMessage scMessage) {
                       loadConfig(scMessage);
                    }
                });
    }

    private void registerDevice() {
        SCMessageOuterClass.SCMessage registerConfigMsg = SCMessageOuterClass.SCMessage.newBuilder()
                .setAction(SCCommand.CONFIG_REGISTER_DEVICE.value())
                .setPayload(
                        String.format("{\"identifier\": \"%s\", \"device_info\": \"%s\", \"mobile\": \"%s\"}",
                                SpatialConnect.getInstance().getDeviceIdentifier(),
                                SCConfigService.getAndroidVersion(),
                                SCAuthService.getUsername())
                )
                .build();
        publish("/config/register", registerConfigMsg, QoS.EXACTLY_ONCE);
    }

    private void listenForUpdates() {
        listenOnTopic("/config/update").subscribe(new Action1<SCMessageOuterClass.SCMessage>() {
            @Override
            public void call(SCMessageOuterClass.SCMessage scMessage) {
                Log.d("FormStore","action: " + scMessage.getAction());
                SpatialConnect sc = SpatialConnect.getInstance();
                SCConfigService cs = sc.getConfigService();
                SCConfig cachedConfig = cs.getCachedConfig();
                JsonUtilities utilities = new JsonUtilities();

                switch (SCCommand.fromActionNumber(scMessage.getAction())) {
                    case CONFIG_ADD_STORE:
                        try {
                            SCStoreConfig config = SCObjectMapper.getMapper()
                                    .readValue(scMessage.getPayload(), SCStoreConfig.class);
                            cachedConfig.addStore(config);
                            sc.getDataService().registerAndStartStoreByConfig(config);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_UPDATE_STORE:
                        try {
                            SCStoreConfig config = SCObjectMapper.getMapper()
                                    .readValue(scMessage.getPayload(), SCStoreConfig.class);
                            cachedConfig.updateStore(config);
                            sc.getDataService().updateStoresByConfig(config);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_REMOVE_STORE:
                        String storeId = utilities.getMapFromJson(scMessage.getPayload())
                            .get("id").toString();
                        SCDataStore store = sc.getDataService().getStoreById(storeId);
                        cachedConfig.removeStore(storeId);
                        sc.getDataService().unregisterStore(store);
                        break;
                    case CONFIG_ADD_FORM:
                        try {
                            SCFormConfig config = SCObjectMapper.getMapper()
                                    .readValue(scMessage.getPayload(), SCFormConfig.class);
                            cachedConfig.addForm(config);
                            sc.getDataService().getFormStore().registerFormByConfig(config);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_UPDATE_FORM:
                        try {
                            SCFormConfig config = SCObjectMapper.getMapper()
                                    .readValue(scMessage.getPayload(), SCFormConfig.class);
                            cachedConfig.updateForm(config);
                            sc.getDataService().getFormStore().updateFormByConfig(config);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_REMOVE_FORM:
                        String formKey = utilities.getMapFromJson(scMessage.getPayload())
                            .get("form_key").toString();
                        cachedConfig.removeForm(formKey);
                        sc.getDataService().getFormStore().unregisterFormByKey(formKey);
                        break;
                }

                cs.setCachedConfig(cachedConfig);
            }
        });
    }

    /**
     * Subscribes to an MQTT topic and returns an Observable with messages received on that topic.
     *
     * @param topic topic to subscribe to
     * @return Observable of {@link SCMessageOuterClass.SCMessage}s published to the topic
     */
    public Observable<SCMessageOuterClass.SCMessage> listenOnTopic(final String topic) {
        mqttHandler.subscribe(topic, QoS.EXACTLY_ONCE.value());
        // filter messages for this topic
        return mqttHandler.scMessageSubject
                .filter(new Func1<Map<String, SCMessageOuterClass.SCMessage>, Boolean>() {
                    @Override
                    public Boolean call(Map<String, SCMessageOuterClass.SCMessage> stringSCMessageMap) {
                        return stringSCMessageMap.keySet().contains(topic);
                    }
                })
                .flatMap(new Func1<Map<String, SCMessageOuterClass.SCMessage>, Observable<SCMessageOuterClass.SCMessage>>() {
                    @Override
                    public Observable<SCMessageOuterClass.SCMessage> call(Map<String, SCMessageOuterClass.SCMessage> stringSCMessageMap) {
                        return Observable.just(stringSCMessageMap.get(topic));
                    }
                });
    }

    /**
     * Publish a message on a topic and listen for the response message.
     *
     * @param topic   topic to publish to
     * @param message SCMessage with the action and payload
     * @return Observable of the {@link SCMessageOuterClass.SCMessage} filtered by the correlation id
     */
    public Observable<SCMessageOuterClass.SCMessage> publishReplyTo(
            String topic,
            final SCMessageOuterClass.SCMessage message) {

        // set the correlation id and replyTo topic
        int correlationId = (int) System.currentTimeMillis();
        final SCMessageOuterClass.SCMessage newMessage = SCMessageOuterClass.SCMessage.newBuilder()
                .setAction(message.getAction())
                .setPayload(message.getPayload())
                .setReplyTo(MqttHandler.REPLY_TO_TOPIC)
                .setCorrelationId(correlationId)
                .setJwt(getJwt())
                .setTime(getTimestamp())
                .build();
        mqttHandler.publish(topic, newMessage, QoS.EXACTLY_ONCE.value());
        // filter message from reply to topic on the correlation id
        return listenOnTopic(MqttHandler.REPLY_TO_TOPIC)
                .filter(new Func1<SCMessageOuterClass.SCMessage, Boolean>() {
                    @Override
                    public Boolean call(SCMessageOuterClass.SCMessage incomingMessage) {
                        return incomingMessage.getCorrelationId() == newMessage.getCorrelationId();
                    }
                })
                .flatMap(new Func1<SCMessageOuterClass.SCMessage, Observable<SCMessageOuterClass.SCMessage>>() {
                    @Override
                    public Observable<SCMessageOuterClass.SCMessage> call(SCMessageOuterClass.SCMessage message) {
                        return Observable.just(message);
                    }
                });
    }

    public void publish(String topic, SCMessageOuterClass.SCMessage message, QoS qos) {



        SCMessageOuterClass.SCMessage.Builder jwtMessagebuilder =  SCMessageOuterClass.SCMessage.newBuilder();
        jwtMessagebuilder.setAction(message.getAction())
                .setPayload(message.getPayload())
                .setReplyTo(message.getReplyTo())
                .setJwt(getJwt())
                .setTime(getTimestamp());

        mqttHandler.publish(topic, jwtMessagebuilder.build(), qos.value());
    }

    private void loadConfig(SCMessageOuterClass.SCMessage message) {
        Log.d(LOG_TAG, "mqtt message received on thread " + Thread.currentThread().getName());
        try {
            SCConfig config = SCObjectMapper.getMapper().readValue(
                    message.getPayload(),
                    SCConfig.class
            );
            Log.d(LOG_TAG, "Loading config received from mqtt broker");
            configReceived.onNext(true);
            SpatialConnect.getInstance().getConfigService().setCachedConfig(config);
            SpatialConnect.getInstance().getConfigService().loadConfig(config);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getJwt() {
        String jwt = SCAuthService.getAccessToken();
        return (jwt != null) ? SCAuthService.getAccessToken() : "";
    }

    public void reconnect() {
        //re subscribe to mqtt topics
        setupSubscriptions();
        listenForUpdates();
    }

    public void addNotification(SCNotification notification) {
        Log.e(LOG_TAG,"add notification");
        notifications.onNext(notification);
    }

    private Timestamp getTimestamp() {
        long millis = System.currentTimeMillis();
        return Timestamp.newBuilder()
            .setSeconds(millis / 1000)
            .setNanos((int) ((millis % 1000) * 1000000))
            .build();
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

    public static String serviceId() {
        return SERVICE_NAME;
    }
}
