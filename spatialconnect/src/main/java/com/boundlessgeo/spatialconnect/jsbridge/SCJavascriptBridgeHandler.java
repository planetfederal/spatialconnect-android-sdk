package com.boundlessgeo.spatialconnect.jsbridge;

import android.location.Location;
import android.util.Base64;
import android.util.Log;

import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryFactory;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCKeyTuple;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


/**
 * WVJBHandler implementation for handling SpatialConnect messages sent from the JS bridge.  You can checkout the
 * handlers <a href="https://github.com/boundlessgeo/spatialconnect-js/blob/development/src/sc.js">here</a>
 */
public class SCJavascriptBridgeHandler implements WebViewJavascriptBridge.WVJBHandler {

    private final String LOG_TAG = SCJavascriptBridgeHandler.class.getSimpleName();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private SCServiceManager manager;
    private WebViewJavascriptBridge bridge;

    public SCJavascriptBridgeHandler(SCServiceManager manager) {
        this.manager = manager;
    }

    protected void setBridge(WebViewJavascriptBridge bridge) {
      this.bridge = bridge;
    }

    static {
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
        Log.d(LOG_TAG, "Received message from bridge: " + data);
        if (data == null && data.equals("undefined")) {
            Log.w(LOG_TAG, "data message was null or undefined");
            return;
        } else {
            JsonNode bridgeMessage = getBridgeMessage(data);
            Integer actionNumber = getActionNumber(bridgeMessage);
            BridgeCommand command = BridgeCommand.fromActionNumber(actionNumber);

            if (command.equals(BridgeCommand.SENSORSERVICE_GPS)) {
                SCSensorService sensorService = manager.getSensorService();
                Integer payloadNumber = getPayloadNumber(bridgeMessage);

                if (payloadNumber == 1) {
                    sensorService.startGPSListener();
                    sensorService.getLastKnownLocation()
                            .subscribeOn(Schedulers.newThread())
                            .subscribe(new Action1<Location>() {
                                @Override
                                public void call(Location location) {
                                    bridge.callHandler("lastKnownLocation",
                                            "{\"latitude\":\"" + location.getLatitude() + "\"," +
                                                    "\"longitude\":\"" + location.getLongitude() + "\"}");
                                }
                            });
                    return;
                }
                if (payloadNumber == 0) {
                    sensorService.disableGPSListener();
                    return;
                }
            }
            if (command.equals(BridgeCommand.DATASERVICE_ACTIVESTORESLIST)) {
                List<SCDataStore> stores = manager.getDataService().getActiveStores();
                StringBuilder sb = new StringBuilder();
                for (SCDataStore store : stores) {
                    if (sb.length() != 0) {
                        sb.append(",");
                    }
                    sb.append("{").append("\"storeid\":\"").append(store.getStoreId()).append("\",");
                    sb.append("\"name\":\"").append(store.toString()).append("\"}");
                }
                bridge.callHandler("storesList", "{\"stores\": [" + sb.toString() + "]}");
                return;
            }
            if (command.equals(BridgeCommand.DATASERVICE_ACTIVESTOREBYID)) {
                String storeId = getStoreId(bridgeMessage);
                String dataStoreString = null;
                try {
                    dataStoreString = MAPPER.writeValueAsString(manager.getDataService().getStoreById(storeId));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return;
                }
                bridge.callHandler("store", dataStoreString);
                return;
            }
            if (command.equals(BridgeCommand.DATASERVICE_GEOSPATIALQUERYALL) || command.equals(BridgeCommand.DATASERVICE_SPATIALQUERYALL)) {
                SCQueryFilter filter = getFilter(bridgeMessage);
                if (filter != null) {
                    manager.getDataService().queryAllStores(filter)
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    new Subscriber<SCSpatialFeature>() {
                                        @Override
                                        public void onCompleted() {
                                            Log.d("BridgeHandler", "query observable completed");
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            e.printStackTrace();
                                            Log.e("BridgeHandler", "onError()\n" + e.getLocalizedMessage());
                                        }

                                        @Override
                                        public void onNext(SCSpatialFeature feature) {
                                            // base64 encode id and set it before sending across wire
                                            try {
                                                String storeId = Base64.encodeToString(
                                                        feature.getKey().getStoreId().getBytes("UTF-8"),
                                                        Base64.DEFAULT
                                                );
                                                String layerId = Base64.encodeToString(
                                                        feature.getKey().getLayerId().getBytes("UTF-8"),
                                                        Base64.DEFAULT
                                                );
                                                String featureId = Base64.encodeToString(
                                                        feature.getKey().getFeatureId().getBytes("UTF-8"),
                                                        Base64.DEFAULT
                                                );
                                                feature.setId(
                                                        String.format("%s.%s.%s", storeId, layerId, featureId)
                                                );
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }
                                            bridge.callHandler("spatialQuery", ((SCGeometry) feature).toJson());
                                        }
                                    }
                            );
                }
            }
            if (command.equals(BridgeCommand.DATASERVICE_UPDATEFEATURE)) {
              SCKeyTuple featureKey = getFeatureKey(bridgeMessage);

              manager.getDataService().getStoreById(featureKey.getStoreId())
                        .update(getFeature(bridgeMessage))
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                new Subscriber<Boolean>() {
                                    @Override
                                    public void onCompleted() {
                                        Log.d("BridgeHandler", "update completed");
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        e.printStackTrace();
                                        Log.e("BridgeHandler", "onError()\n" + e.getLocalizedMessage());
                                    }

                                    @Override
                                    public void onNext(Boolean updated) {
                                        Log.d("BridgeHandler", "feature updated!");
                                    }
                                }
                        );
            }
            if (command.equals(BridgeCommand.DATASERVICE_DELETEFEATURE)) {
                SCKeyTuple featureKey = getFeatureKey(bridgeMessage);
                manager.getDataService().getStoreById(featureKey.getStoreId())
                        .delete(featureKey)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                new Subscriber<Boolean>() {
                                    @Override
                                    public void onCompleted() {
                                        Log.d("BridgeHandler", "delete completed");
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        e.printStackTrace();
                                        Log.e("BridgeHandler", "onError()\n" + e.getLocalizedMessage());
                                    }

                                    @Override
                                    public void onNext(Boolean updated) {
                                        Log.d("BridgeHandler", "feature deleted!");
                                    }
                                }
                        );


            }
        }
    }

    private SCKeyTuple getFeatureKey(JsonNode payload) {
        // see: https://github.com/boundlessgeo/spatialconnect-js/blob/development/src/sc.js#L95
        return new SCKeyTuple(
                payload.get("payload").asText().split("\\.")[0],
                payload.get("payload").asText().split("\\.")[1],
                payload.get("payload").asText().split("\\.")[2]
        );
    }

    private SCSpatialFeature getFeature(JsonNode payload) {
        String featureString = payload.get("payload").get("feature").asText();
        return new SCGeometryFactory().getSpatialFeatureFromFeatureJson(featureString);
    }

    // gets either a 1 or a 0 indicating turn on/off something
    private Integer getPayloadNumber(JsonNode payload) {
        return payload.get("payload").asInt();
    }

    private String getStoreId(JsonNode payload) {
        return payload.get("payload").get("storeId").asText();
    }

    private Integer getActionNumber(JsonNode payload) {
        return payload.get("action").asInt();
    }

    private JsonNode getBridgeMessage(String payload) {
        try {
            return MAPPER.readTree(payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SCQueryFilter getFilter(JsonNode payload) {
        // TODO: we need to have a BridgeMessage object so we centralize the JSON --> POJO mappings
        JsonNode bboxNode = payload.get("payload").get("filter").get("$geocontains");
        try {
            List<Integer> points = MAPPER.readValue(bboxNode.traverse(), new TypeReference<List<Integer>>() {
            });
            SCBoundingBox bbox = new SCBoundingBox(
                    points.get(0),
                    points.get(1),
                    points.get(2),
                    points.get(3)
            );
            SCQueryFilter filter = new SCQueryFilter(
                    new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
            );
            return filter;
        } catch (IOException e) {
            Log.e(LOG_TAG, "couldn't build filter...check the syntax of your bbox: " + bboxNode.textValue());
            e.printStackTrace();
        }
        return null;
    }
}
