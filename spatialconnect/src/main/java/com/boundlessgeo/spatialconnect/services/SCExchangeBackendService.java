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
package com.boundlessgeo.spatialconnect.services;


import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCConfig;
import com.boundlessgeo.spatialconnect.config.SCRemoteConfig;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.mqtt.SCNotification;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.services.authService.SCAuthService;
import com.boundlessgeo.spatialconnect.services.authService.SCExchangeAuthMethod;
import com.boundlessgeo.spatialconnect.stores.ISyncableStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.vividsolutions.jts.geom.Geometry;

import java.util.List;
import java.util.Map;

import okhttp3.Credentials;
import okhttp3.Response;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

import static java.util.Arrays.asList;

/**
 * SCExchangeBackendService extends SCBackendService to enable Boundless Exchange to serve as a backend to the
 * spatialconnect-android-sdk.
 */
public class SCExchangeBackendService extends SCBackendService {

    private static final String LOG_TAG = SCExchangeBackendService.class.getSimpleName();
    private static final String SERVICE_NAME = "SC_EXCHANGE_BACKEND_SERVICE";
    private Context context;
    private Observable<SCNotification> notifications;
    private SCAuthService authService;
    private SCConfigService configService;
    private SCSensorService sensorService;
    private SCDataService dataService;
    private String backendUri;

    /**
     * BehaviorSubject that emits True when the SCConfig has been received
     */
    public BehaviorSubject<Boolean> configReceived = BehaviorSubject.create(false);

    public SCExchangeBackendService(final Context context) {
        this.context = context;
    }

    @Override
    public String getId() {
        return SERVICE_NAME;
    }

    @Override
    public List<String> getRequires() {
        return asList(SCAuthService.serviceId(), SCSensorService.serviceId(), SCDataService.serviceId(),
                SCConfigService.serviceId());
    }

    /**
     * Initialize the backend service with a {@code SCRemoteConfig} to setup connections to the
     * backend.
     *
     * @param config
     */
    public void initialize(SCRemoteConfig config) {
        backendUri = String.format(
                "%s://%s:%s",
                config.getHttpProtocol() == null ? "http" : config.getHttpProtocol(),
                config.getHttpHost() == null ? "localhost" : config.getHttpHost(),
                config.getHttpPort() == null ? "8000" : config.getHttpPort().toString()
        );
    }

    @Override
    public boolean start(Map<String, SCService> deps) {
        boolean started = super.start(deps);
        authService = (SCAuthService) deps.get(SCAuthService.serviceId());
        configService = (SCConfigService) deps.get(SCConfigService.serviceId());
        sensorService = (SCSensorService) deps.get(SCSensorService.serviceId());
        dataService = (SCDataService) deps.get(SCDataService.serviceId());

        // load the config from cache if any is present
        loadCachedConfig();

        // load config from http backend
        loadConfig();

        // listen to sync events from data service
        listenForSyncEvents();

        return started;
    }


    /**
     * Observable emitting SCNotifications
     *
     * @return Observable
     */
    public Observable<SCNotification> getNotifications() {
        return notifications;
    }

    private void loadCachedConfig() {
        configService = SpatialConnect.getInstance().getConfigService();
        SCConfig config = configService.getCachedConfig();
        if (config != null) {
            configService.loadConfig(config);
            configReceived.onNext(true);
        }
    }

    private void loadConfig() {
        // make request to Exchange to get layers and turn them into forms
        sensorService.isConnected().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean connected) {
                if (connected) {
                    Log.v(LOG_TAG, String.format("Network connected...fetching config from %s", backendUri));
                    configReceived.onNext(true);
                }
            }
        });
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
        sensorService.isConnected().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean connected) {
                if (connected) {
                    Log.d(LOG_TAG, "Sending feature to backend" + feature.toJson());

                    // todo: wrap feature in WFS-T and POST it to Exchange's GeoServer
                    String wfstPayload = buildWFSTInsertPayload(feature);

                    SCExchangeAuthMethod auth =
                            (SCExchangeAuthMethod) SpatialConnect.getInstance()
                                    .getAuthService()
                                    .getAuthMethod();

                    try {
                        Log.d(LOG_TAG, "Executing WFS-T Insert request");
                        Response res = HttpHandler.getInstance().postBlocking(
                                backendUri,
                                wfstPayload,
                                Credentials.basic(auth.username(), auth.getPassword()),
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
                    + "      %3$s\n"
                    + "      %4$s\n"
                    + "    </%2$s>\n"
                    + "  </wfs:Insert>\n"
                    + "</wfs:Transaction>";

    private static final String POINT_XML_TEMPLATE =
            "      <%1$s>\n"
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
        String featureTypeUrl = String.format("%sgeoserver/wfs/DescribeFeatureType?typename=%s:%s",
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
}
