/**
 * Copyright 2017 Boundless, http://boundlessgeo.com
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
package com.boundlessgeo.spatialconnect.services.backendService;


import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCConfig;
import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCFormField;
import com.boundlessgeo.spatialconnect.config.SCRemoteConfig;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.mqtt.SCNotification;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.scutilities.Json.SCObjectMapper;
import com.boundlessgeo.spatialconnect.scutilities.WFSParser;
import com.boundlessgeo.spatialconnect.scutilities.WFSUtils;
import com.boundlessgeo.spatialconnect.services.SCConfigService;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCService;
import com.boundlessgeo.spatialconnect.services.authService.SCAuthService;
import com.boundlessgeo.spatialconnect.stores.ISCSpatialStore;
import com.boundlessgeo.spatialconnect.stores.ISyncableStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.sync.SyncItem;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * SCExchangeBackendService to enable Boundless Exchange to serve as a backend to the
 * spatialconnect-android-sdk.
 */
public class SCExchangeBackend implements ISCBackend {

    private static final String LOG_TAG = SCExchangeBackend.class.getSimpleName();
    private final int AUDIT_OP_CREATE = 1;
    private final int AUDIT_OP_UPDATE = 2;
    private final int AUDIT_OP_DELETE = 3;

    private SCConfigService configService;
    private SCSensorService sensorService;
    private SCDataService dataService;
    private SCAuthService authService;
    private String backendUri;

    /**
     * Create the backend service with a {@code SCRemoteConfig} to setup connections to the backend.
     *
     * @param config
     */
    public SCExchangeBackend(SCRemoteConfig config) {
        backendUri = String.format(
                "%s://%s:%s",
                config.getHttpProtocol() == null ? "http" : config.getHttpProtocol(),
                config.getHttpHost() == null ? "localhost" : config.getHttpHost(),
                config.getHttpPort() == null ? "80" : config.getHttpPort().toString()
        );
    }

    @Override
    public void start(Map<String, SCService> deps) {
        configService = (SCConfigService) deps.get(SCConfigService.serviceId());
        sensorService = (SCSensorService) deps.get(SCSensorService.serviceId());
        dataService = (SCDataService) deps.get(SCDataService.serviceId());
        authService = (SCAuthService) deps.get(SCAuthService.serviceId());

        // load the config from cache if any is present
        loadCachedConfig();

        // load config from http backend
        loadConfig();

        // listen to sync events from data service
        listenForSyncEvents();
    }

    /**
     * Observable emitting SCNotifications
     *
     * @return Observable
     */
    @Override
    public Observable<SCNotification> getNotifications() {
        throw new UnsupportedOperationException("This method is not implemented yet");
    }

    @Override
    public void updateDeviceToken(String token) {
        throw new UnsupportedOperationException("This method is not implemented yet");
    }

    private void loadCachedConfig() {
        configService = SpatialConnect.getInstance().getConfigService();
        SCConfig config = configService.getCachedConfig();
        if (config != null) {
            configService.loadConfig(config);
            SCBackendService.configReceived.onNext(true);
        }
    }

    public void loadConfig() {
        // make request to Exchange to get layers and turn them into forms
        Observable.combineLatest(
                sensorService.isConnected(),
                authService.getLoginStatus(),
                new Func2<Boolean, Integer, Boolean>() {
                    @Override
                    public Boolean call(Boolean isConnected, Integer loginStatus) {
                        return isConnected && loginStatus == SCAuthService.SCAuthStatus.AUTHENTICATED.value();
                    }
                })
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean connected) {
                        if (connected) {
                            Log.v(LOG_TAG, String.format("Fetching config from %s", backendUri));
                            List<String> layerNames = fetchLayerNames();
                            for (String layerName : layerNames) {
                                SCFormConfig formConfig = buildSCFormConfig(layerName);
                                configService.addForm(formConfig);
                            }
                            SCBackendService.configReceived.onNext(true);
                        }
                    }
                });
    }

    // fetches a list of editable layer names from exchange
    private List<String> fetchLayerNames() {
        List<String> layerNames = new ArrayList<>();
        OkHttpClient client = HttpHandler.getInstance().getClient();
        Request request = new Request.Builder()
                .url(String.format("%s/gs/acls", backendUri))
                .header("Authorization", "Bearer " + authService.getAccessToken())
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                JsonNode root = SCObjectMapper.getMapper().readTree(response.body().string());
                Iterator<JsonNode> rwLayers = root.get("rw").iterator();
                while (rwLayers.hasNext()) {
                    layerNames.add(rwLayers.next().asText());
                }
            }
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Could not fetch layer names", e);
        }
        return layerNames;
    }

    private SCFormConfig buildSCFormConfig(String layerName) {
        SCFormConfig formConfig = new SCFormConfig();
        OkHttpClient client = HttpHandler.getInstance().getClient();
        Request request = new Request.Builder()
                .url(String.format("%s/layers/%s/get", backendUri, layerName))
                .header("Authorization", "Bearer " + authService.getAccessToken())
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                JsonNode root = SCObjectMapper.getMapper().readTree(response.body().string());
                formConfig.setId(UUID.randomUUID().toString());
                formConfig.setFormKey(layerName);
                formConfig.setFormLabel(root.get("title").asText());
                formConfig.setFields(buildFormFields(root));
            }
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Could not fetch layer names", e);
        }
        return formConfig;
    }

    private  List<HashMap<String, Object>> buildFormFields(JsonNode root) {
        Iterator<JsonNode> attributes = root.get("attributes").iterator();
        List<HashMap<String, Object>> formFields = new ArrayList<>();
        while (attributes.hasNext()) {

            JsonNode attribute = attributes.next();
            Iterator<String> fieldNames = attribute.fieldNames();

            HashMap<String, Object> formField = new HashMap<>();
            formField.put(SCFormField.FIELD_KEY, attribute.get("attribute").asText());
            if (!attribute.has("attribute_label") || attribute.get("attribute_label") == null) {
                formField.put(SCFormField.FIELD_LABEL, attribute.get("attribute_label").asText());
            } else {
                formField.put(SCFormField.FIELD_LABEL, attribute.get("attribute").asText());
            }
            formField.put(SCFormField.TYPE, getFieldType(attribute));
            formField.put(SCFormField.POSITION,  attribute.get("display_order").asInt());
            int visible = attribute.get("visible").asBoolean() ? 1: 0;
            if (attribute.get("attribute_type").asText().contains("gml")) {
                visible = 0;
            }
            formField.put(SCFormField.VISIBLE, visible);

            formFields.add(formField);
        }
        return formFields;
    }

    private String getFieldType(JsonNode attribute) {
        // if attribute name is "photos" then the type should be photo
        if (attribute.get("attribute").asText().equalsIgnoreCase("photos")) {
            return "photo";
        }
        if (attribute.get("attribute_type").asText().contains("gml")) {
            return "geometry";
        }
        switch (attribute.get("attribute_type").asText()) {
            case "xsd:string":
                return "string";
            case "xsd:int":
            case "xsd:double":
            case "xsd:long":
                return "number";
            case "xsd:dateTime":
            case "xsd:date":
                return "date";
            default:
                return "string";
        }
    }

    private void listenForSyncEvents() {
        Observable<SCDataStore> syncableStores = dataService.getISyncableStores();

        Observable<Object> storeEditSync =
                syncableStores.flatMap(new Func1<SCDataStore, Observable<SCSpatialFeature>>() {
                    @Override
                    public Observable<SCSpatialFeature> call(SCDataStore scDataStore) {
                        return scDataStore.storeEdited;
                    }
                }).flatMap(new Func1<SCSpatialFeature, Observable<?>>() {
                    @Override
                    public Observable call(SCSpatialFeature feature) {
                        final ISyncableStore store = (ISyncableStore) dataService
                                .getStoreByIdentifier(feature.getStoreId());
                        return syncStore(store);
                    }
                });

        //sync all stores when connection to broker is true
        Observable<Object> onlineSync = sensorService.isConnected().filter(new Func1<Boolean, Boolean>() {
            @Override
            public Boolean call(Boolean sync) {
                return sync;
            }
            // for each connected State true (sync is true), sync the stores
        }).flatMap(new Func1<Boolean, Observable<?>>() {
            @Override
            public Observable call(Boolean sync) {
                return syncStores();
            }
        });

        Observable<Object> sync = onlineSync.mergeWith(storeEditSync);
        sync.subscribeOn(Schedulers.io()).subscribe(new Action1<Object>() {
            @Override
            public void call(Object object) {

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.e(LOG_TAG, throwable.getLocalizedMessage());
            }
        });
    }

    private Observable syncStores() {
        return dataService.getISyncableStores().flatMap(new Func1<SCDataStore, Observable<?>>() {
            @Override
            public Observable call(SCDataStore scDataStore) {
                return syncStore((ISyncableStore) scDataStore);
            }
        });
    }

    private Observable syncStore(final ISyncableStore store) {
        Log.d(LOG_TAG, "Syncing store channel " + store.syncChannel());

        return store.unSent().flatMap(new Func1<SyncItem, Observable<?>>() {
            @Override
            public Observable call(final SyncItem syncItem) {
                return send(syncItem);
            }
        });
    }

    private Observable send(final SyncItem syncItem) {
         return Observable.combineLatest(
                sensorService.isConnected(),
                authService.getLoginStatus(),
                new Func2<Boolean, Integer, Boolean>() {
                    @Override
                    public Boolean call(Boolean isConnected, Integer loginStatus) {
                        return isConnected && loginStatus == SCAuthService.SCAuthStatus.AUTHENTICATED.value();
                    }
                }).flatMap(new Func1<Boolean, Observable<?>>() {
             @Override
             public Observable call(Boolean connected) {
                if (connected) {
                    String wfstPayload = "";
                    String url = String.format("%s/geoserver/wfs", backendUri);
                    if (syncItem.getOperation() == AUDIT_OP_CREATE) {
                        wfstPayload = WFSUtils.buildWFSTInsertPayload(syncItem.getFeature(), url);
                    } else if (syncItem.getOperation() == AUDIT_OP_UPDATE) {
                        wfstPayload = WFSUtils.buildWFSTUpdatePayload(syncItem.getFeature(), url);
                    } else if (syncItem.getOperation() == AUDIT_OP_DELETE) {
                        wfstPayload = WFSUtils.buildWFSTDeletePayload(syncItem.getFeature(), url);
                    }

                    try {
                        Response res = HttpHandler.getInstance().postBlocking(
                                url,
                                wfstPayload,
                                String.format("Bearer %s", authService.getAccessToken()),
                                HttpHandler.XML);

                        WFSParser wfsParser = new WFSParser(res.body().byteStream());
                        if (wfsParser.isSuccess()) {
                            final ISyncableStore store =
                                    (ISyncableStore) dataService.getStoreByIdentifier(syncItem.getFeature().getStoreId());
                            store.updateAuditTable(syncItem.getFeature());

                            //Feature id comes back as layer_name.<id> for NON geogig layers
                            Map<String, Object> properties = new HashMap<>();
                            properties.put("id", wfsParser.getFeatureId().substring(wfsParser.getFeatureId().indexOf(".") + 1));
                            syncItem.getFeature().setProperties(properties);

                            ((ISCSpatialStore)store).update(syncItem.getFeature())
                                    .subscribe(new Action1<SCSpatialFeature>() {
                                        @Override
                                        public void call(SCSpatialFeature scSpatialFeature) {

                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable t) {
                                            Log.e(LOG_TAG, "Something when wrong update the local gpkg id: " + t.getMessage());
                                        }
                                    }, new Action0() {
                                        @Override
                                        public void call() {
                                            Log.d(LOG_TAG, "updated local gkpk id succesuffly after WFST insert");
                                        }
                                    });

                            return Observable.empty();
                        } else {
                            String error = String.format("Did not successfully send feature %s to backend",
                                    syncItem.getFeature().getKey().encodedCompositeKey());
                            Log.e(LOG_TAG,
                                    "Something went wrong sending to server: " +
                                            error);
                            return Observable.error(new Throwable(error));
                        }
                    }
                    catch (Exception e) {
                        Log.e(LOG_TAG, String.format("Did not successfully send feature %s to backend",
                                syncItem.getFeature().getId()), e);
                        return Observable.error(e);
                    }
                } else {
                    return Observable.empty();
                }
             }
         });
    }

    public String getBackendUri() {
        return backendUri;
    }

    public Observable<Boolean> isConnected() {
     return SpatialConnect.getInstance().getSensorService().isConnected().asObservable();
    }
}
