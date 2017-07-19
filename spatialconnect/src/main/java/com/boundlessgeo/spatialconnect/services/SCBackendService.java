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
import android.os.Build;
import android.util.Log;

import com.boundlessgeo.schema.Actions;
import com.boundlessgeo.schema.MessagePbf;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCConfig;
import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCRemoteConfig;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.mqtt.MqttHandler;
import com.boundlessgeo.spatialconnect.mqtt.QoS;
import com.boundlessgeo.spatialconnect.mqtt.SCNotification;
import com.boundlessgeo.spatialconnect.scutilities.Json.JsonUtilities;
import com.boundlessgeo.spatialconnect.scutilities.SCTuple;
import com.boundlessgeo.spatialconnect.services.authService.SCAuthService;
import com.boundlessgeo.spatialconnect.stores.ISyncableStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.Timestamp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

import static com.boundlessgeo.spatialconnect.scutilities.Json.SCObjectMapper.getMapper;
import static java.util.Arrays.asList;

/**
 * SCBackendService handles any communication with backend SpatialConnect services.
 */
public class SCBackendService extends SCService implements SCServiceLifecycle {

    private static final String LOG_TAG = SCBackendService.class.getSimpleName();
    private static final String SERVICE_NAME = "SC_BACKEND_SERVICE";
    private Context context;
    private MqttHandler mqttHandler;
    private Observable<SCNotification> notifications;
    private Observable<Boolean> syncStores;
    private SCAuthService authService;
    private SCConfigService configService;
    private SCSensorService sensorService;
    private SCDataService dataService;

    /**
     * Behavior Observable emitting True when the SpatialConnect SCConfig has been received
     */
    public BehaviorSubject<Boolean> configReceived = BehaviorSubject.create(false);

    /**
     * Behavior Observable emitting True when Connected, False when the Connection is down
     */
    public BehaviorSubject<Boolean> connectedToBroker = BehaviorSubject.create(false);
    /**
     * Endpoint running SpatialConnect Server
     */
    public String backendUri = null;

    public SCBackendService(final Context context) {
        this.context = context;
        this.mqttHandler = MqttHandler.getInstance(context);
    }

    /**
     * Initialize the backend service with a {@code SCRemoteConfig} to setup connections to the
     * SpatialConnect backend including the REST API and the MQTT broker.
     *
     * @param config
     */
    public void initialize(SCRemoteConfig config) {
        if (backendUri == null) {
            backendUri = String.format(
                    "%s://%s:%s",
                    config.getHttpProtocol() == null ? "http" : config.getHttpProtocol(),
                    config.getHttpHost() == null ? "localhost" : config.getHttpHost(),
                    config.getHttpPort() == null ? "8085" : config.getHttpPort().toString()
            );
        }
        mqttHandler.initialize(config);
        setupMqttConnectionListener();
    }

    /**
     * Observable emiting SCNotifications
     * @return Observable<{@link SCRemoteConfig}>
     */
    public Observable<SCNotification> getNotifications() {
        return notifications;
    }

    /**
     * Publishes an SCMessage to the SpatialConnect Server
     * @param topic topic MQTT destination topic
     * @param message msg {@link MessagePbf.Msg} to be sent
     */
    public void publish(String topic, MessagePbf.Msg message) {
        MessagePbf.Msg.Builder msgBuilder =  MessagePbf.Msg.newBuilder();
        msgBuilder.setAction(message.getAction())
                .setPayload(message.getPayload())
                .setTo(message.getTo())
                .setJwt(getJwt());

        mqttHandler.publish(topic, msgBuilder.build(), QoS.EXACTLY_ONCE.value());
    }

    /**
     * Publishes an SCMessage to the SpatialConnect Server with At Most Once Delivery QoS 0
     * @param topic topic MQTT destination topic
     * @param message msg {@link MessagePbf.Msg} to be sent
     */
    public void publishAtMostOnce(String topic, MessagePbf.Msg message) {
        MessagePbf.Msg.Builder msgBuilder =  MessagePbf.Msg.newBuilder();
        msgBuilder.setAction(message.getAction())
                .setPayload(message.getPayload())
                .setTo(message.getTo())
                .setJwt(getJwt());

        mqttHandler.publish(topic, msgBuilder.build(), QoS.AT_MOST_ONCE.value());
    }

    /**
     * Publishes an SCMessage to the SpatialConnect Server with At Least Once Delivery QoS 1
     * @param topic topic MQTT destination topic
     * @param message msg {@link MessagePbf.Msg} to be sent
     */
    public void publishAtLeastOnce(String topic, MessagePbf.Msg message) {
        MessagePbf.Msg.Builder msgBuilder =  MessagePbf.Msg.newBuilder();
        msgBuilder.setAction(message.getAction())
                .setPayload(message.getPayload())
                .setTo(message.getTo())
                .setJwt(getJwt());

        mqttHandler.publish(topic, msgBuilder.build(), QoS.AT_LEAST_ONCE.value());
    }

    /**
     * Publishes an SCMessage to the SpatialConnect Server with Exactly Once Delivery QoS 2
     * @param topic topic MQTT destination topic
     * @param message msg {@link MessagePbf.Msg} to be sent
     */
    public void publishExactlyOnce(String topic, MessagePbf.Msg message) {
        MessagePbf.Msg.Builder msgBuilder =  MessagePbf.Msg.newBuilder();
        msgBuilder.setAction(message.getAction())
                .setPayload(message.getPayload())
                .setTo(message.getTo())
                .setJwt(getJwt());

        mqttHandler.publish(topic, msgBuilder.build(), QoS.EXACTLY_ONCE.value());
    }

    /**
     * Publishes a message with a reply-to observable returned for creating a request
     * reply with the server.
     *
     * @param topic topic MQTT destination topic
     * @param message msg {@link MessagePbf.Msg} to be sent
     * @return Observable of the {@link MessagePbf.Msg} filtered by the correlation id
     */
    public Observable<MessagePbf.Msg> publishReplyTo(
            String topic,
            final MessagePbf.Msg message) {

        // set the correlation id and replyTo topic
        long correlationId = System.currentTimeMillis();
        final MessagePbf.Msg newMessage = MessagePbf.Msg.newBuilder()
                .setAction(message.getAction())
                .setPayload(message.getPayload())
                .setTo(MqttHandler.REPLY_TO_TOPIC)
                .setCorrelationId(correlationId)
                .setJwt(getJwt())
                .build();
        mqttHandler.publish(topic, newMessage, QoS.EXACTLY_ONCE.value());
        // filter message from reply to topic on the correlation id
        return listenOnTopic(MqttHandler.REPLY_TO_TOPIC)
                .filter(new Func1<MessagePbf.Msg, Boolean>() {
                    @Override
                    public Boolean call(MessagePbf.Msg incomingMessage) {
                        return incomingMessage.getCorrelationId() == newMessage.getCorrelationId();
                    }
                })
                .flatMap(new Func1<MessagePbf.Msg, Observable<MessagePbf.Msg>>() {
                    @Override
                    public Observable<MessagePbf.Msg> call(MessagePbf.Msg message) {
                        return Observable.just(message);
                    }
                });
    }

    /**
     * Subscribes to an MQTT Topic
     *
     * @param topic topic to listen on
     * @return Observable of {@link MessagePbf.Msg}
     * filtered to only receive messages from the stated topic
     */
    public Observable<MessagePbf.Msg> listenOnTopic(final String topic) {
        mqttHandler.subscribe(topic, QoS.EXACTLY_ONCE.value());
        // filter messages for this topic
        return mqttHandler.getMulticast()
                .filter(new Func1<SCTuple, Boolean>() {
                    @Override
                    public Boolean call(SCTuple tuple) {
                        return tuple.first().toString().equalsIgnoreCase(topic);
                    }
                })
                .map(new Func1<SCTuple, MessagePbf.Msg>() {
                    @Override
                    public MessagePbf.Msg call(SCTuple scTuple) {
                        return (MessagePbf.Msg) scTuple.second();
                    }
                });
    }

    public String getJwt() {
        SCAuthService authService = SpatialConnect.getInstance().getAuthService();
        String jwt = authService.getAccessToken();
        return (jwt != null) ? authService.getAccessToken() : "";
    }

    public void reconnect() {
        //re subscribe to mqtt topics
        registerForLocalNotifications();
        setupSubscriptions();
    }

    @Override
    public boolean start(Map<String, SCService> deps) {
        authService = (SCAuthService)deps.get(SCAuthService.serviceId());
        configService = (SCConfigService) deps.get(SCConfigService.serviceId());
        sensorService = (SCSensorService) deps.get(SCSensorService.serviceId());
        dataService = (SCDataService) deps.get(SCDataService.serviceId());
        listenForNetworkConnection();
        return super.start(deps);
    }

    @Override
    public String getId() {
        return SERVICE_NAME;
    }

    @Override
    public List<String> getRequires() {
        return asList(SCAuthService.serviceId(), SCConfigService.serviceId(),
                SCSensorService.serviceId(), SCDataService.serviceId());
    }

    private void connectToMqttBroker() {
        mqttHandler.connect();
        connectedToBroker
                .filter(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean connected) {
                        return connected;
                    }
                })
                .take(1)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        setupSubscriptions();
                        registerAndFetchConfig();
                        listenForSyncEvents();
                    }
                });
    }

    private void loadCachedConfig () {
        SCConfig config = configService.getCachedConfig();
        if (config != null) {
            configService.loadConfig(config);
            configReceived.onNext(true);
        }
    }

    private void authListener() {
        Log.d(LOG_TAG, "waiting for authentication before connecting to mqtt broker");
        authService.getLoginStatus().subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer status) {
                if (status == SCAuthService.SCAuthStatus.AUTHENTICATED.value()) {
                    connectToMqttBroker();
                }
                if (status == SCAuthService.SCAuthStatus.AUTHENTICATION_FAILED.value()) {
                    loadCachedConfig();
                }
            }
        });
    }

    private void listenForNetworkConnection() {
        Log.d(LOG_TAG, "Subscribing to network connectivity updates.");
        SCSensorService sensorService = SpatialConnect.getInstance().getSensorService();
        sensorService.isConnected().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean connected) {
                if (connected) {
                    authListener();
                } else {
                    //load config from cache
                    Log.d(LOG_TAG, "No internet get cached remote config");
                    connectedToBroker.onNext(false);
                    loadCachedConfig();
                }
            }
        });
    }

    private void loadConfig(MessagePbf.Msg message) {
        try {
            SCConfig config = getMapper().readValue(
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

    private void registerAndFetchConfig() {
        registerDevice();
        fetchConfig();
    }

    private void fetchConfig() {
        Log.d(LOG_TAG, "fetching config from mqtt config topic");
        MessagePbf.Msg getConfigMsg = MessagePbf.Msg.newBuilder()
                .setAction(Actions.CONFIG_FULL.value()).build();

        publishReplyTo("/config", getConfigMsg)
                .subscribe(new Action1<MessagePbf.Msg>() {
                    @Override
                    public void call(MessagePbf.Msg message) {
                        loadConfig(message);
                    }
                });
    }

    private void registerDevice() {

        SpatialConnect sc = SpatialConnect.getInstance();
        MessagePbf.Msg registerConfigMsg = MessagePbf.Msg.newBuilder()
                .setAction(Actions.CONFIG_REGISTER_DEVICE.value())
                .setPayload(
                        String.format("{\"identifier\": \"%s\", \"device_info\": %s, \"name\": \"mobile:%s\"}",
                                sc.getDeviceIdentifier(),
                                getAndroidVersion(),
                                authService.getUsername())
                )
                .build();
        publishExactlyOnce("/config/register", registerConfigMsg);
    }

    private void setupSubscriptions() {
        notifications = listenOnTopic("/notify")
                .mergeWith(listenOnTopic(String.format("/notify/%s", SpatialConnect.getInstance().getDeviceIdentifier())))
                .map(new Func1<MessagePbf.Msg, SCNotification>() {
                    @Override
                    public SCNotification call(MessagePbf.Msg msg) {
                        return new SCNotification(msg);
                    }
                });

        listenOnTopic("/config/update").subscribe(new Action1<MessagePbf.Msg>() {
            @Override
            public void call(MessagePbf.Msg msg) {
                Log.d("FormStore","action: " + msg.getAction());
                SCConfig cachedConfig = configService.getCachedConfig();
                JsonUtilities utilities = new JsonUtilities();

                switch (Actions.fromAction(msg.getAction())) {
                    case CONFIG_ADD_STORE:
                        try {
                            SCStoreConfig config = getMapper()
                                    .readValue(msg.getPayload(), SCStoreConfig.class);
                            cachedConfig.addStore(config);
                            dataService.registerAndStartStoreByConfig(config);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_UPDATE_STORE:
                        try {
                            SCStoreConfig config = getMapper()
                                    .readValue(msg.getPayload(), SCStoreConfig.class);
                            cachedConfig.updateStore(config);
                            dataService.updateStoresByConfig(config);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_REMOVE_STORE:
                        String storeId = utilities.getMapFromJson(msg.getPayload())
                                .get("id").toString();
                        SCDataStore store = dataService.getStoreByIdentifier(storeId);
                        cachedConfig.removeStore(storeId);
                        dataService.unregisterStore(store);
                        break;
                    case CONFIG_ADD_FORM:
                        try {
                            SCFormConfig config = getMapper()
                                    .readValue(msg.getPayload(), SCFormConfig.class);
                            cachedConfig.addForm(config);
                            dataService.getFormStore().registerFormByConfig(config);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_UPDATE_FORM:
                        try {
                            SCFormConfig config = getMapper()
                                    .readValue(msg.getPayload(), SCFormConfig.class);
                            cachedConfig.updateForm(config);
                            dataService.getFormStore().updateFormByConfig(config);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CONFIG_REMOVE_FORM:
                        String formKey = utilities.getMapFromJson(msg.getPayload())
                                .get("form_key").toString();
                        cachedConfig.removeForm(formKey);
                        dataService.getFormStore().unregisterFormByKey(formKey);
                        break;
                }

                configService.setCachedConfig(cachedConfig);
            }
        });
    }

    private void registerForLocalNotifications() {
        notifications = listenOnTopic("/notify")
                .mergeWith(listenOnTopic(String.format("/notify/%s", SpatialConnect.getInstance().getDeviceIdentifier())))
                .map(new Func1<MessagePbf.Msg, SCNotification>() {
                    @Override
                    public SCNotification call(MessagePbf.Msg msg) {
                        return new SCNotification(msg);
                    }
                });
    }

    private void setupMqttConnectionListener() {
        Log.d(LOG_TAG, "setting up mqtt connection listener");
        MqttHandler.clientConnected.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean clientConnected) {
                connectedToBroker.onNext(clientConnected);
            }
        });
    }

    private Timestamp getTimestamp() {
        long millis = System.currentTimeMillis();
        return Timestamp.newBuilder()
                .setSeconds(millis / 1000)
                .setNanos((int) ((millis % 1000) * 1000000))
                .build();
    }

    private String getAndroidVersion() {
        String release = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        String os =  String.format("Android SDK: %s (%s)", sdkVersion, release);
        return String.format("{\"os\": \"%s\"}", os);
    }

    private void listenForSyncEvents() {

        Observable<SCDataStore> syncableStores = dataService.getISyncableStores();

        Observable<Boolean>  storeEditSync = syncableStores
                .flatMap(new Func1<SCDataStore, Observable<SCSpatialFeature>>() {
                    @Override
                    public Observable<SCSpatialFeature> call(SCDataStore scDataStore) {
                        return scDataStore.storeEdited;
                    }
                }).map(new Func1<SCSpatialFeature, Boolean>() {
                    @Override
                    public Boolean call(SCSpatialFeature feature) {
                        final ISyncableStore store = (ISyncableStore) dataService.getStoreByIdentifier(feature.getStoreId());
                        syncStore(store);
                        return null;
                    }
                });

        //sync all stores when connection to broker is true
        Observable<Boolean> onlineSync = connectedToBroker
                .filter(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean sync) {
                        return sync;
                    }
                }).map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean sync) {
                        syncStores();
                        return sync;
                    }
                });

        Observable<Boolean> sync = onlineSync.mergeWith(storeEditSync);
        sync.subscribeOn(Schedulers.newThread()).subscribe();
    }

    private void syncStores() {

        dataService.getISyncableStores()
                .subscribe(new Action1<SCDataStore>() {
                    @Override
                    public void call(SCDataStore scDataStore) {
                        syncStore((ISyncableStore) scDataStore);
                    }
                });
    }

    private void syncStore(final ISyncableStore store) {
        Log.d(LOG_TAG, "Syncing store channel " + store.syncChannel());
        store.unSent().subscribe(new Action1<SCSpatialFeature>() {
            @Override
            public void call(final SCSpatialFeature scSpatialFeature) {
                send(scSpatialFeature);
            }
        });
    }

    private void send(final SCSpatialFeature feature) {
        connectedToBroker
            .filter(new Func1<Boolean, Boolean>() {
                @Override public Boolean call(Boolean connected) {
                    return connected;
                }
            })
            .subscribe(new Action1<Boolean>() {
                @Override public void call(Boolean connected) {
                    Log.d(LOG_TAG, "Sending feature to mqtt broker " + feature.toJson());
                    final ISyncableStore store =
                        (ISyncableStore) dataService.getStoreByIdentifier(feature.getStoreId());
                    Map<String, Object> featurePayload = store.generateSendPayload(feature);
                    String payload;
                    try {
                        payload = getMapper().writeValueAsString(featurePayload);
                        MessagePbf.Msg message = MessagePbf.Msg.newBuilder()
                            .setAction(Actions.DATASERVICE_CREATEFEATURE.value())
                            .setPayload(payload)
                            .build();
                        publishReplyTo(store.syncChannel(), message)
                            .subscribe(new Action1<MessagePbf.Msg>() {
                                @Override public void call(MessagePbf.Msg message) {
                                    try {
                                        final JSONObject payload = new JSONObject(message.getPayload());
                                        if (payload.getBoolean("result")) {
                                            Log.d(LOG_TAG, "update audit table to remove sent feature");
                                            store.updateAuditTable(feature);
                                        } else {
                                            Log.e(LOG_TAG,
                                                "Something went wrong sending to server: " + payload
                                                    .getString("error"));
                                        }
                                    } catch (JSONException je) {
                                        Log.e(LOG_TAG, "json parse error: " + je.getMessage());
                                    }
                                }
                            });
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            });
    }

    public static String serviceId() {
        return SERVICE_NAME;
    }
}
