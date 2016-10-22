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
import android.net.NetworkInfo;
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
import com.boundlessgeo.spatialconnect.scutilities.Json.ObjectMappers;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.github.pwittchen.reactivenetwork.library.Connectivity;
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork;

import java.io.IOException;
import java.util.Map;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

/**
 * SCBackendService handles any communication with backend SpatialConnect services.
 */
public class SCBackendService extends SCService {

    private static final String LOG_TAG = SCBackendService.class.getSimpleName();
    private static Context context;
    private static MqttHandler mqttHandler;
    private Observable<SCNotification> notifications;
    public static BehaviorSubject<Boolean> configReceived = BehaviorSubject.create(false);
    public static BehaviorSubject<Boolean> networkConnected = BehaviorSubject.create(false);
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
    }

    @Override
    public void start() {
        mqttHandler.connect();
        Log.d(LOG_TAG, "Subscribing to network connectivity updates.");
        ReactiveNetwork.observeNetworkConnectivity(context)
                .subscribe(new Action1<Connectivity>() {
                    @Override
                    public void call(Connectivity connectivity) {
                        Log.d(LOG_TAG, "Connectivity is " + connectivity.getState().name());
                        networkConnected.onNext(connectivity.getState().equals(NetworkInfo.State.CONNECTED));
                    }
                });

        networkConnected.subscribe(new Action1<Boolean>() {
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
                                }
                            }
                        });
                    } else {
                        //load config from cache
                        Log.d(LOG_TAG, "No internet get cached remote config");
                        SpatialConnect.getInstance().getConfigService().loadConfigFromCache();

                    }


                }
            });
    }

    private void setupSubscriptions() {
        notifications = listenOnTopic("/notify")
                .flatMap(new Func1<SCMessageOuterClass.SCMessage, Observable<SCNotification>>() {
                    @Override
                    public Observable<SCNotification> call(SCMessageOuterClass.SCMessage message) {
                        SCNotification notification = new SCNotification(message);
                        Log.d(LOG_TAG, "received notification message:" + notification.toJson().toString());
                        return Observable.just(notification);
                    }
                });
    }

    public Observable<SCNotification> getNotifications() {
        return notifications;
    }

    private void registerAndFetchConfig() {
        registerDevice();
        fetchConfig();
        listenForUpdates();
    }

    private void fetchConfig() {
        Log.e(LOG_TAG, "fetching config from mqtt config topic");
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
                        String.format("{\"identifier\": \"%s\", \"device_info\": \"%s\"}",
                                SCConfigService.getClientId(),
                                SCConfigService.getAndroidVersion())
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

                switch (SCCommand.fromActionNumber(scMessage.getAction())) {
                    case CONFIG_ADD_STORE:
                        try {
                            SCStoreConfig config = ObjectMappers.getMapper()
                                    .readValue(scMessage.getPayload(), SCStoreConfig.class);
                            cachedConfig.addStore(config);
                            sc.getDataService().registerAndStartStoreByConfig(config);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_UPDATE_STORE:
                        try {
                            SCStoreConfig config = ObjectMappers.getMapper()
                                    .readValue(scMessage.getPayload(), SCStoreConfig.class);
                            cachedConfig.updateStore(config);
                            sc.getDataService().updateStoresByConfig(config);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_REMOVE_STORE:
                        SCDataStore store = sc.getDataService().getStoreById(scMessage.getPayload());
                        cachedConfig.removeStore(scMessage.getPayload());
                        sc.getDataService().unregisterStore(store);
                        break;
                    case CONFIG_ADD_FORM:
                        try {
                            SCFormConfig config = ObjectMappers.getMapper()
                                    .readValue(scMessage.getPayload(), SCFormConfig.class);
                            cachedConfig.addForm(config);
                            sc.getDataService().getFormStore().registerFormByConfig(config);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_UPDATE_FORM:
                        try {
                            SCFormConfig config = ObjectMappers.getMapper()
                                    .readValue(scMessage.getPayload(), SCFormConfig.class);
                            cachedConfig.updateForm(config);
                            sc.getDataService().getFormStore().updateFormByConfig(config);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_REMOVE_FORM:
                            cachedConfig.removeForm(scMessage.getPayload());
                            sc.getDataService()
                                    .getFormStore()
                                    .unregisterFormByKey(scMessage.getPayload());
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
        int correlationId = (int) (System.currentTimeMillis() / 1000L);
        final SCMessageOuterClass.SCMessage newMessage = SCMessageOuterClass.SCMessage.newBuilder()
                .setAction(message.getAction())
                .setPayload(message.getPayload())
                .setReplyTo(MqttHandler.REPLY_TO_TOPIC)
                .setCorrelationId(correlationId)
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
        mqttHandler.publish(topic, message, qos.value());
    }

    private void loadConfig(SCMessageOuterClass.SCMessage message) {
        Log.d(LOG_TAG, "mqtt message received on thread " + Thread.currentThread().getName());
        try {
            SCConfig config = ObjectMappers.getMapper().readValue(
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
}
