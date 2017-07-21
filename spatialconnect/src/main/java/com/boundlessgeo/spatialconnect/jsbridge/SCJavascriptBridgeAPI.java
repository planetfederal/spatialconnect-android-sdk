package com.boundlessgeo.spatialconnect.jsbridge;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import com.boundlessgeo.schema.Actions;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryFactory;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.mqtt.SCNotification;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.scutilities.Json.JsonUtilities;
import com.boundlessgeo.spatialconnect.scutilities.Json.SCObjectMapper;
import com.boundlessgeo.spatialconnect.services.SCBackendService;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCServiceStatusEvent;
import com.boundlessgeo.spatialconnect.services.authService.SCAuthService;
import com.boundlessgeo.spatialconnect.stores.ISCSpatialStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCKeyTuple;
import com.boundlessgeo.spatialconnect.stores.SCRasterStore;
import com.boundlessgeo.spatialconnect.stores.SCStoreStatusEvent;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class SCJavascriptBridgeAPI {

    private static final String TAG = SCJavascriptBridgeAPI.class.getSimpleName();

    private SpatialConnect mSpatialConnect;

    /*
     * main purpose of this wrapper is to standardize/centralize the behavior of calling onCompleted(),
     * onError(), onNext(). This is functionally equivalent to implementing these methods in anonymous classes
     * in each .subscribe() call
     */
    private class SubscriberWrapper<T> extends Subscriber<T> {

        Subscriber<Object> mSubscriber;

        SubscriberWrapper(Subscriber<Object> subscriber) {
            mSubscriber = subscriber;
        }

        @Override
        public void onCompleted() {
            mSubscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            mSubscriber.onError(e);
        }

        @Override
        public void onNext(T o) {
            mSubscriber.onNext(o);
        }
    }

    SCJavascriptBridgeAPI(Context context) {
        mSpatialConnect = SpatialConnect.getInstance();
        mSpatialConnect.initialize(context);
    }

    /**
     * Handles a message sent from Javascript.  Expects the message envelope to look like:
     * <code>{"type":<String>,"payload":<JSON Object>}</code>
     *
     * @param jsonMessage message received from Javascript
     */
    public void parseJSAction(HashMap<String, Object> jsonMessage, Subscriber<Object> subscriber) {
        if (jsonMessage == null) {
            subscriber.onCompleted();
            return;
        }

        // parse bridge message to determine command
        String type = JsonUtilities.getString(jsonMessage, "type");
        if (TextUtils.isEmpty(type)) {
            return;
        }
        Actions command = Actions.fromAction(type);

        Log.d(TAG, "sdk handling: " + command + " :: " + jsonMessage);
        if (command == Actions.START_ALL_SERVICES) {
            handleStartAllServices();
        } else if (command == Actions.DATASERVICE_ACTIVESTORESLIST) {
            handleActiveStoresList(subscriber);
        } else if (command == Actions.DATASERVICE_ACTIVESTOREBYID) {
            handleActiveStoreById(JsonUtilities.getHashMap(jsonMessage, "payload"), subscriber);
        } else if (command == Actions.DATASERVICE_STORELIST) {
            handleStoreList(subscriber);
        } else if (command == Actions.DATASERVICE_QUERY
                || command == Actions.DATASERVICE_SPATIALQUERY) {
            handleQueryStoresByIds(JsonUtilities.getHashMap(jsonMessage, "payload"), subscriber);
        } else if (command == Actions.DATASERVICE_QUERYALL
                || command == Actions.DATASERVICE_SPATIALQUERYALL) {
            handleQueryAllStores(JsonUtilities.getHashMap(jsonMessage, "payload"), subscriber);
        } else if (command == Actions.DATASERVICE_CREATEFEATURE) {
            handleCreateFeature(JsonUtilities.getHashMap(jsonMessage, "payload"), subscriber);
        } else if (command == Actions.DATASERVICE_UPDATEFEATURE) {
            handleUpdateFeature(JsonUtilities.getHashMap(jsonMessage, "payload"), subscriber);
        } else if (command == Actions.DATASERVICE_DELETEFEATURE) {
            handleDeleteFeature(JsonUtilities.getString(jsonMessage, "payload"), subscriber);
        } else if (command == Actions.DATASERVICE_FORMLIST) {
            handleFormsList(subscriber);
        } else if (command == Actions.SENSORSERVICE_GPS) {
            handleSensorServiceGps(JsonUtilities.getInt(jsonMessage, "payload"), subscriber);
        } else if (command == Actions.AUTHSERVICE_AUTHENTICATE) {
            handleAuthenticate(JsonUtilities.getHashMap(jsonMessage, "payload"), subscriber);
        } else if (command == Actions.AUTHSERVICE_LOGOUT) {
            handleLogout(subscriber);
        } else if (command == Actions.AUTHSERVICE_ACCESS_TOKEN) {
            handleAccessToken(subscriber);
        } else if (command == Actions.AUTHSERVICE_LOGIN_STATUS) {
            handleLoginStatus(subscriber);
        } else if (command == Actions.NOTIFICATIONS) {
            handleNotificationSubscribe(subscriber);
        } else if (command == Actions.BACKENDSERVICE_HTTP_URI) {
            handleBackendServiceHTTPUri(subscriber);
        } else if (command == Actions.BACKENDSERVICE_MQTT_CONNECTED) {
            handleMqttConnectionStatus(subscriber);
        }
    }

    /**
     * Handles the {@link Actions#START_ALL_SERVICES} command.
     */
    private void handleStartAllServices() {
        mSpatialConnect.startAllServices();
    }

    /**
     * Handles the {@link Actions#DATASERVICE_ACTIVESTORESLIST} command.
     */
    private void handleActiveStoresList(Subscriber<Object> subscriber) {
        mSpatialConnect.getDataService()
                .hasStores
                .subscribe(new SubscriberWrapper<Boolean>(subscriber) {
                    @Override
                    public void onNext(Boolean hasStores){
                        if (hasStores) {
                            HashMap<String, Object> payload = new HashMap<>();

                            List<SCDataStore> stores = mSpatialConnect.getDataService().getActiveStores();
                            ArrayList<HashMap<String, Object>> storesArray = new ArrayList<>();
                            for (SCDataStore store : stores) {
                                storesArray.add(getStoreMap(store));
                            }

                            payload.put("stores", storesArray);

                            mSubscriber.onNext(payload);
                        }
                    }
                } );
    }

    /**
     * Handles all the {@link Actions#DATASERVICE_ACTIVESTOREBYID} commands.
     */
    private void handleActiveStoreById(HashMap<String, Object> payload, Subscriber<Object> subscriber) {
        String storeId = JsonUtilities.getString(payload, "storeId");
        SCDataStore store = mSpatialConnect.getDataService().getStoreByIdentifier(storeId);
        SubscriberWrapper<Object> wrapper = new SubscriberWrapper<>(subscriber);
        wrapper.onNext(getStoreMap(store));
        wrapper.onCompleted();
    }

    /**
     * Handles all the {@link Actions#DATASERVICE_STORELIST} commands.
     */
    private void handleStoreList(Subscriber<Object> subscriber) {
        SubscriberWrapper<Object> wrapper = new SubscriberWrapper<Object>(subscriber) {
            // this method used to/can receive a SCStoreStatusEvent, but doesnt actually report that value
            @Override
            public void onNext(Object object) {
                mSubscriber.onNext(getAllStoresPayload());
            }
        };

        wrapper.onNext(getAllStoresPayload());

        mSpatialConnect.getDataService().getStoreEvents().subscribe(wrapper);
    }

    /**
     * Handles the {@link Actions#DATASERVICE_QUERYALL} and
     * {@link Actions#DATASERVICE_SPATIALQUERYALL} commands.
     */
    private void handleQueryAllStores(HashMap<String, Object> payload, Subscriber<Object> subscriber) {
        SCQueryFilter filter = getFilter(JsonUtilities.getHashMap(payload, "filter"));

        mSpatialConnect.getDataService().queryAllStores(filter)
                       .subscribeOn(Schedulers.io())
                       .subscribe(new SubscriberWrapper<SCSpatialFeature>(subscriber) {
                           @Override
                           public void onNext(SCSpatialFeature feature) {
                               try {
                                   // base64 encode id and set it before sending across wire
                                   String encodedId = feature.getKey().encodedCompositeKey();
                                   feature.setId(encodedId);
                                   mSubscriber.onNext(feature.toJSON());
                               } catch (UnsupportedEncodingException e) {
                                   mSubscriber.onError(e);
                               }
                           }
                       });
    }

    /**
     * Handles the {@link Actions#DATASERVICE_QUERY} and
     * {@link Actions#DATASERVICE_SPATIALQUERY} commands.
     */
    private void handleQueryStoresByIds(HashMap<String, Object> payload, Subscriber<Object> subscriber) {
        SCQueryFilter filter = getFilter(JsonUtilities.getHashMap(payload, "filter"));

        ArrayList<String> storeIds = JsonUtilities.getArrayList(payload, "storeId", String.class);

        mSpatialConnect.getDataService().queryStoresByIds(storeIds, filter)
                .subscribeOn(Schedulers.io())
                .subscribe(new SubscriberWrapper<SCSpatialFeature>(subscriber) {
                    @Override
                    public void onNext(SCSpatialFeature feature) {
                        try {
                            // base64 encode id and set it before sending across wire
                            String encodedId = feature.getKey().encodedCompositeKey();
                            feature.setId(encodedId);
                            mSubscriber.onNext(feature.toJSON());
                        } catch (UnsupportedEncodingException e) {
                            mSubscriber.onError(e);
                        }
                    }
                });
    }

    /**
     * Handles the {@link Actions#DATASERVICE_CREATEFEATURE} command.
     */
    private void handleCreateFeature(HashMap<String, Object> payload, Subscriber<Object> subscriber) {
        String featureString = null;
        try {
            featureString = SCObjectMapper.getMapper().writeValueAsString(JsonUtilities.getHashMap(payload, "feature"));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        SCSpatialFeature newFeature = new SCGeometryFactory().getSpatialFeatureFromFeatureJson(featureString);
        // if no store was specified, use the default store
        if (newFeature.getKey().getStoreId() == null || newFeature.getKey().getStoreId().isEmpty()) {
            newFeature.setStoreId(newFeature.getKey().getStoreId());
        }
        SCDataStore store = mSpatialConnect.getDataService().getStoreByIdentifier(newFeature.getKey().getStoreId());
        ((ISCSpatialStore) store).create(newFeature)
                .subscribeOn(Schedulers.io())
                .subscribe(new SubscriberWrapper<SCSpatialFeature>(subscriber) {
                    @Override
                    public void onNext(SCSpatialFeature feature) {
                                try {
                                    // base64 encode id and set it before sending across wire
                                    String encodedId = feature.getKey().encodedCompositeKey();
                                    feature.setId(encodedId);
                                    mSubscriber.onNext(feature.toJSON());
                                } catch (UnsupportedEncodingException e) {
                                    mSubscriber.onError(e);
                                }
                            }
                        });
    }

    /**
     * Handles the {@link Actions#DATASERVICE_UPDATEFEATURE} command.
     */
    private void handleUpdateFeature(HashMap<String, Object> payload, Subscriber<Object> subscriber) {
        String featureString = null;
        try {
            featureString = SCObjectMapper.getMapper().writeValueAsString(JsonUtilities.getHashMap(payload, "feature"));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        SubscriberWrapper wrapper = new SubscriberWrapper(subscriber);

        SCSpatialFeature feature = new SCGeometryFactory().getSpatialFeatureFromFeatureJson(featureString);
        try {
            SCKeyTuple decodedTuple = new SCKeyTuple(feature.getId());
            // update feature with decoded values
            feature.setStoreId(decodedTuple.getStoreId());
            feature.setLayerId(decodedTuple.getLayerId());
            feature.setId(decodedTuple.getFeatureId());
        } catch (UnsupportedEncodingException e) {
            wrapper.onError(e);
        }

        SCDataStore store = mSpatialConnect.getDataService().getStoreByIdentifier(feature.getKey().getStoreId());
        ((ISCSpatialStore) store).update(feature).subscribeOn(Schedulers.io()).subscribe(wrapper);
    }

    /**
     * Handles the {@link Actions#DATASERVICE_DELETEFEATURE} command.
     */
    private void handleDeleteFeature(String payload, Subscriber<Object> subscriber) {
        SubscriberWrapper wrapper = new SubscriberWrapper(subscriber);

        try {
            SCKeyTuple featureKey = new SCKeyTuple(payload);
            SCDataStore store = mSpatialConnect.getDataService().getStoreByIdentifier(featureKey.getStoreId());
            ((ISCSpatialStore) store).delete(featureKey).subscribeOn(Schedulers.io()).subscribe(wrapper);
        } catch (UnsupportedEncodingException e) {
            wrapper.onError(e);
        }
    }

    /**
     * Handles the {@link Actions#DATASERVICE_FORMLIST} command.
     */
    private void handleFormsList(Subscriber<Object> subscriber) {
        mSpatialConnect.getDataService()
                .getFormStore()
                .hasForms
                .subscribe(new SubscriberWrapper<Boolean>(subscriber) {
                    @Override
                    public void onNext(Boolean hasForms) {
                        if (hasForms) {
                            HashMap<String, Object> payload = new HashMap<>();

                            List<SCFormConfig> formConfigs =
                                    mSpatialConnect.getDataService().getFormStore().getFormConfigs();
                            ArrayList<HashMap<String, Object>> formsArray = new ArrayList<>();
                            for (SCFormConfig config : formConfigs) {
                                formsArray.add(config.toJSON());
                            }

                            payload.put("forms", formsArray);

                            this.mSubscriber.onNext(payload);
                        }
                    }
                });

    }

    /**
     * Handles all the {@link Actions#SENSORSERVICE_GPS} commands.
     */
    private void handleSensorServiceGps(int payload, Subscriber<Object> subscriber) {
        SCSensorService sensorService = mSpatialConnect.getSensorService();
        if (payload == 1) {
            sensorService.enableGPS();
            sensorService.getLastKnownLocation()
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(new SubscriberWrapper<Location>(subscriber) {
                        @Override
                        public void onNext(Location location) {
                            HashMap<String, Object> payload = new HashMap<>();

                            payload.put("latitude", location.getLatitude());
                            payload.put("longitude", location.getLongitude());
                            payload.put("altitude", location.getAltitude());

                            this.mSubscriber.onNext(payload);
                        }
                    });
        }
        if (payload == 0) {
            sensorService.disableGPS();
        }
    }

    private void handleAuthenticate(HashMap<String, Object> payload, Subscriber<Object> subscriber) {
        String email = JsonUtilities.getString(payload, "email");
        String password = JsonUtilities.getString(payload, "password");
        SCAuthService authService = SpatialConnect.getInstance().getAuthService();
        authService.authenticate(email, password);
        (new SubscriberWrapper(subscriber)).onCompleted();
    }

    private void handleLogout(Subscriber<Object> subscriber) {
        SpatialConnect.getInstance().getAuthService().logout();
        (new SubscriberWrapper(subscriber)).onCompleted();
    }

    private void handleAccessToken(Subscriber<Object> subscriber) {
        SCAuthService authService = SpatialConnect.getInstance().getAuthService();
        String accessToken = authService.getAccessToken();
        SubscriberWrapper<Object> wrapper = new SubscriberWrapper<>(subscriber);
        if (accessToken != null) {
            wrapper.onNext(accessToken);
        }
        wrapper.onCompleted();
    }

    /**
     * Handles the {@link Actions#AUTHSERVICE_LOGIN_STATUS} command.
     */
    private void handleLoginStatus(final Subscriber<Object> subscriber) {
        mSpatialConnect.serviceRunning(SCAuthService.serviceId())
                .subscribe(new SubscriberWrapper<SCServiceStatusEvent>(subscriber) {
                    @Override
                    public void onNext(SCServiceStatusEvent scServiceStatusEvent) {
                        SCAuthService authService = SpatialConnect.getInstance().getAuthService();
                        authService.getLoginStatus().subscribe(new SubscriberWrapper<Integer>(subscriber));
                    }
                });
    }

    private void handleNotificationSubscribe(final Subscriber<Object> subscriber) {
        SCSensorService sensorService = SpatialConnect.getInstance().getSensorService();
        sensorService.isConnected().subscribe(new SubscriberWrapper<Boolean>(subscriber) {
            @Override
            public void onNext(Boolean connected) {
                if (connected) {
                    final SpatialConnect sc = SpatialConnect.getInstance();
                    sc.serviceRunning(SCBackendService.serviceId())
                            .subscribe(new SubscriberWrapper<SCServiceStatusEvent>(subscriber) {
                                @Override
                                public void onNext(SCServiceStatusEvent scServiceStatusEvent) {
                                    sc.getBackendService()
                                            .getNotifications()
                                            .subscribe(new SubscriberWrapper<SCNotification>(subscriber) {
                                                @Override
                                                public void onNext(SCNotification scNotification) {
                                                    this.mSubscriber.onNext(scNotification.toJSON());
                                                }
                                            });
                                }
                            });
                }
            }
        });

    }

    /**
     * Handles the {@link Actions#BACKENDSERVICE_HTTP_URI} command.
     */
    private void handleBackendServiceHTTPUri(Subscriber<Object> subscriber) {
        mSpatialConnect.serviceRunning(SCBackendService.serviceId())
                .subscribe(new SubscriberWrapper<SCServiceStatusEvent>(subscriber) {
                    @Override
                    public void onNext(SCServiceStatusEvent scServiceStatusEvent) {
                        HashMap<String, Object> payload = new HashMap<>();
                        payload.put("backendUri", mSpatialConnect.getBackendService().backendUri + "/api/");

                        this.mSubscriber.onNext(payload);
                    }
                });

    }

    /**
     * Handles the {@link Actions#BACKENDSERVICE_MQTT_CONNECTED} command.
     */
    private void handleMqttConnectionStatus(Subscriber<Object> subscriber) {
        SubscriberWrapper<Boolean> wrapper = new SubscriberWrapper<Boolean>(subscriber) {
            @Override
            public void onNext(Boolean connected) {
                HashMap<String, Object> payload = new HashMap<>();
                payload.put("connected", connected);

                this.mSubscriber.onNext(payload);
            }
        };

        SpatialConnect sc = SpatialConnect.getInstance();
        SCBackendService backendService = sc.getBackendService();
        if (backendService != null) {
            backendService
                    .connectedToBroker
                    .subscribeOn(Schedulers.io())
                    .subscribe(wrapper);
        } else {
            wrapper.onNext(false);
        }
    }

    private HashMap<String, Object> getStoreMap(SCDataStore store) {
        HashMap<String, Object> json = store.toJSON();

        if (store instanceof ISCSpatialStore) {
            json.put("vectorLayers", ((ISCSpatialStore) store).vectorLayers());
        }
        if (store instanceof SCRasterStore) {
            json.put("rasterLayers", ((SCRasterStore) store).rasterLayers());
        }

        return json;
    }

    private HashMap<String, Object> getAllStoresPayload() {
        HashMap<String, Object> payload = new HashMap<>();
        ArrayList<HashMap<String, Object>> storesArray = new ArrayList<>();

        List<SCDataStore> stores = mSpatialConnect.getDataService().getStoreList();
        for (SCDataStore store : stores) {
            storesArray.add(getStoreMap(store));
        }
        payload.put("stores", storesArray);

        return payload;
    }

    // builds a query filter based on the filter in payload
    private SCQueryFilter getFilter(HashMap<String, Object> filterJSON) {
        SCBoundingBox bbox;

        ArrayList<Double> coords = JsonUtilities.getArrayList(filterJSON, "$geocontains", Double.class);
        // if a bounding box was provided, use it
        if (coords != null && coords.size() >= 3) {
            bbox = new SCBoundingBox(coords.get(0), coords.get(1), coords.get(2), coords.get(3));
        }
        // otherwise use the world
        else {
            bbox = new SCBoundingBox(-180, -90, 180, 90);
        }

        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN));

        // add layers to filter
        if (filterJSON.containsKey("layers")) {
            filter.addLayerIds(JsonUtilities.getArrayList(filterJSON, "layers", String.class));
        }

        // add limit
        if (filterJSON.containsKey("limit")) {
            filter.setLimit(JsonUtilities.getInt(filterJSON, "limit"));
        }
        return filter;
    }
}
