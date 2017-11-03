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
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.mqtt.SCNotification;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.scutilities.Json.SCObjectMapper;
import com.boundlessgeo.spatialconnect.services.SCConfigService;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCService;
import com.boundlessgeo.spatialconnect.services.authService.SCAuthService;
import com.boundlessgeo.spatialconnect.stores.ISyncableStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.vividsolutions.jts.geom.Geometry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * SCExchangeBackendService to enable Boundless Exchange to serve as a backend to the
 * spatialconnect-android-sdk.
 */
public class SCExchangeBackend implements ISCBackend {

    private static final String LOG_TAG = SCExchangeBackend.class.getSimpleName();
    private static final String SERVICE_NAME = "SC_EXCHANGE_BACKEND_SERVICE";
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
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                JsonNode value = attribute.get(key);
                if (value.isInt()) {
                    formField.put(key, value.asInt());
                } else if (value.isBoolean()) {
                    formField.put(key, value.asBoolean());
                } else if (value.isNumber() && !value.isInt()) {
                    formField.put(key, value.asDouble());
                } else {
                    formField.put(key, value.asText());
                }
            }
            formFields.add(formField);
        }
        return formFields;
    }

    private String getFieldType(JsonNode attribute) {
        // if attribute name is "photos" then the type should be photo
        if (attribute.get("attribute").asText().equalsIgnoreCase("photos")) {
            return "photo";
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
                return null;
        }
    }

    private void listenForSyncEvents() {
        // sync all stores each time we (re)connect to the backend
        sensorService.isConnected().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean connected) {
                if (connected) {
                    Log.v(LOG_TAG, "Network connected...syncing all stores");
                    syncStores();
                }
            }
        });

        // sync stores each time a new feature is added, updated, or deleted
        dataService.getISyncableStores().flatMap(new Func1<SCDataStore, Observable<SCSpatialFeature>>() {
            @Override
            public Observable<SCSpatialFeature> call(SCDataStore scDataStore) {
                // should the data service have a subject with all updated features across all the syncable stores?
                return scDataStore.storeEdited.asObservable();
            }
        }).forEach(new Action1<SCSpatialFeature>() {
            @Override
            public void call(SCSpatialFeature scSpatialFeature) {
                send(scSpatialFeature);
            }
        });

    }

    private void syncStores() {
        dataService.getISyncableStores().forEach(new Action1<SCDataStore>() {
            @Override
            public void call(SCDataStore scDataStore) {
                ((ISyncableStore) scDataStore).unSent().forEach(new Action1<SCSpatialFeature>() {
                    @Override
                    public void call(SCSpatialFeature scSpatialFeature) {
                        send(scSpatialFeature);
                    }
                });
            }
        });
    }

    private void send(final SCSpatialFeature feature) {
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
                            String wfstPayload = buildWFSTInsertPayload(feature);
                            String wfsUrl = String.format("%s/geoserver/wfs", backendUri);
                            Log.d(LOG_TAG, String.format("Sending WFS-T to %s\n%s", wfsUrl, wfstPayload));
                            try {
                                Response res = HttpHandler.getInstance().postBlocking(
                                        wfsUrl,
                                        wfstPayload,
                                        String.format("Bearer %s", authService.getAccessToken()),
                                        HttpHandler.XML);
                                if (!res.isSuccessful()) {
                                    Log.w(LOG_TAG, String.format("Did not successfully send feature %s to backend",
                                            feature.getKey().encodedCompositeKey()));
                                }
                            }
                            catch (Exception e) {
                                Log.e(LOG_TAG, String.format("Did not successfully send feature %s to backend",
                                        feature.getId()), e);
                            }
                        }
                    }
                });
    }

    private static final String WFST_INSERT_TEMPLATE =
            "<wfs:Transaction service=\"WFS\" version=\"1.0.0\"\n"
                    + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\n"
                    + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "  xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-transaction.xsd %1$s\">\n"
                    + "  <wfs:Insert>\n"
                    + "    <%2$s>\n"
                    + "      %3$s"
                    + "      %4$s"
                    + "    </%2$s>\n"
                    + "  </wfs:Insert>\n"
                    + "</wfs:Transaction>";

    private static final String POINT_XML_TEMPLATE =
            "<%1$s>\n"
                    + "        <gml:Point srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
                    + "          <gml:coordinates decimal=\".\" cs=\",\" ts=\" \">%2$f,%3$f</gml:coordinates>\n"
                    + "        </gml:Point>\n"
                    + "      </%1$s>\n";


    private String buildPropertiesXml(SCSpatialFeature scSpatialFeature) {
        Map<String, Object> properties = scSpatialFeature.getProperties();
        StringBuilder sb = new StringBuilder();
        for (String key : properties.keySet()) {
            if (properties.get(key) != null) {
                sb.append(String.format("<%1$s>%2$s</%1$s>\n", key, properties.get(key)));
            }
        }
        return sb.toString();
    }

    private String buildGeometryXml(SCSpatialFeature scSpatialFeature) {
        if (scSpatialFeature instanceof SCGeometry) {
            if (((SCGeometry) scSpatialFeature).getGeometry() != null) {
                Geometry geom = ((SCGeometry) scSpatialFeature).getGeometry();
                String type = geom.getGeometryType();
                // todo: find geometry column name
                String geometryColumnName = "wkb_geometry";
                if (type.equalsIgnoreCase("point")) {
                    //Because we are using WFS 1.0.0 we specify the order in lon/lat
                    //see http://docs.geoserver.org/stable/en/user/services/wfs/basics.html#wfs-basics-axis
                    return String.format(POINT_XML_TEMPLATE,
                            geometryColumnName,
                            geom.getCoordinate().y,
                            geom.getCoordinate().x);
                }
            }
        }
        Log.w(LOG_TAG, String.format("Feature %s did not have a geometry", scSpatialFeature.getId()));
        return "";
    }

    private String buildWFSTInsertPayload(SCSpatialFeature scSpatialFeature) {
        // assumes "geonode" will always be the GeoServer workspace for Exchange backend
        String featureTypeUrl = String.format("%s/geoserver/wfs/DescribeFeatureType?typename=%s:%s",
                backendUri, "geonode", scSpatialFeature.getLayerId());

        return String.format(WFST_INSERT_TEMPLATE,
                featureTypeUrl,
                scSpatialFeature.getLayerId(),
                buildPropertiesXml(scSpatialFeature),
                buildGeometryXml(scSpatialFeature));
    }

    private Double validateDouble(Object o) {
        Double val = null;
        if (o instanceof Number) {
            val = ((Number) o).doubleValue();
        }
        return val;
    }

    public static String serviceId() {
        return SERVICE_NAME;
    }

    public String getBackendUri() {
        return backendUri;
    }

    public Observable<Boolean> isConnected() {
     return SpatialConnect.getInstance().getSensorService().isConnected().asObservable();
    }
}
