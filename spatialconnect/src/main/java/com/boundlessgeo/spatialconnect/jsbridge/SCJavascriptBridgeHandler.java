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
package com.boundlessgeo.spatialconnect.jsbridge;

import android.location.Location;
import android.util.Log;

import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryFactory;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.schema.SCCommand;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCKeyTuple;
import com.boundlessgeo.spatialconnect.stores.ISCSpatialStore;
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
    private SpatialConnect manager;
    private WebViewJavascriptBridge bridge;

    public SCJavascriptBridgeHandler(SpatialConnect manager) {
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
            SCCommand command = SCCommand.fromActionNumber(actionNumber);

            if (command.equals(SCCommand.SENSORSERVICE_GPS)) {
                SCSensorService sensorService = manager.getSensorService();
                Integer payloadNumber = getPayloadNumber(bridgeMessage);

                if (payloadNumber == 1) {
                    sensorService.enableGPS();
                    sensorService.getLastKnownLocation()
                            .subscribeOn(Schedulers.newThread())
                            .subscribe(new Action1<Location>() {
                                @Override
                                public void call(Location location) {
                                    bridge.callHandler("lastKnownLocation",
                                            "{\"lat\":\"" + location.getLatitude() + "\"," +
                                                    "\"lon\":\"" + location.getLongitude() + "\"}");
                                }
                            });
                    return;
                }
                if (payloadNumber == 0) {
                    sensorService.disableGPS();
                    return;
                }
            }
            if (command.equals(SCCommand.DATASERVICE_ACTIVESTORESLIST)) {
                List<SCDataStore> stores = manager.getDataService().getActiveStores();
                StringBuilder sb = new StringBuilder();
                for (SCDataStore store : stores) {
                    if (sb.length() != 0) {
                        sb.append(",");
                    }
                    sb.append("{");
                    sb.append("\"name\":\"").append(store.getName()).append("\",");
                    sb.append("\"storeId\":\"").append(store.getStoreId()).append("\",");
                    sb.append("\"type\":\"").append(store.getType()).append("\"");
                    sb.append("}");
                }
                bridge.callHandler("storesList", "{\"stores\": [" + sb.toString() + "]}");
                return;
            }
            if (command.equals(SCCommand.DATASERVICE_ACTIVESTOREBYID)) {
                String storeId = getStoreId(bridgeMessage);
                String dataStoreString = null;
                try {
                    dataStoreString = MAPPER.writeValueAsString(manager.getDataService().getStoreByIdentifier(storeId));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return;
                }
                bridge.callHandler("store", dataStoreString);
                return;
            }
            if (command.equals(SCCommand.DATASERVICE_GEOSPATIALQUERYALL) || command.equals(
                SCCommand.DATASERVICE_SPATIALQUERYALL)) {
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
                                          try {
                                            // base64 encode id and set it before sending across wire
                                            String encodedId = ((SCGeometry) feature).getKey().encodedCompositeKey();
                                            feature.setId(encodedId);
                                            bridge.callHandler("spatialQuery", ((SCGeometry) feature).toJson());
                                          } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                          }
                                        }
                                    }
                            );
                }
            }
            if (command.equals(SCCommand.DATASERVICE_UPDATEFEATURE)) {
              try {
                SCSpatialFeature featureToUpdate = getFeatureToUpdate(
                  bridgeMessage.get("payload").get("feature").asText()
                );
                SCDataStore store = manager.getDataService().getStoreByIdentifier(featureToUpdate.getKey().getStoreId());
                  ((ISCSpatialStore) store).update(featureToUpdate)
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
              } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
              }
            }
            if (command.equals(SCCommand.DATASERVICE_DELETEFEATURE)) {
              try {
                SCKeyTuple featureKey = new SCKeyTuple(bridgeMessage.get("payload").asText());
                SCDataStore store = manager.getDataService().getStoreByIdentifier(featureKey.getStoreId());
                  ((ISCSpatialStore) store).delete(featureKey)
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
              } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
              }
            }
            if (command.equals(SCCommand.DATASERVICE_CREATEFEATURE)) {
              try {
                SCSpatialFeature newFeature = getNewFeature(bridgeMessage.get("payload"));
                SCDataStore store = manager.getDataService().getStoreByIdentifier(newFeature.getKey().getStoreId());
                  ((ISCSpatialStore) store).create(newFeature)
                  .subscribeOn(Schedulers.io())
                  .subscribe(
                    new Subscriber<SCSpatialFeature>() {
                      @Override
                      public void onCompleted() {
                        Log.d("BridgeHandler", "create completed");
                      }

                      @Override
                      public void onError(Throwable e) {
                        e.printStackTrace();
                        Log.e("BridgeHandler", "onError()\n" + e.getLocalizedMessage());
                      }

                      @Override
                      public void onNext(SCSpatialFeature feature) {
                        try {
                          // base64 encode id and set it before sending across wire
                          String encodedId = ((SCGeometry) feature).getKey().encodedCompositeKey();
                          feature.setId(encodedId);
                          bridge.callHandler("createFeature", ((SCGeometry) feature).toJson());
                          Log.d("BridgeHandler", "feature created!");
                        } catch (UnsupportedEncodingException e) {
                          e.printStackTrace();
                        }
                      }
                    }
                  );
              } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
              }
            }
        }
    }

    /**
     * Returns an SCSpatialFeature instance based on the GeoJSON Feature string sent from the bridge for update.
     *
     * @param featureString the GeoJSON string representing the feature
     * @return
     * @throws UnsupportedEncodingException
     */
    private SCSpatialFeature getFeatureToUpdate(String featureString) throws UnsupportedEncodingException {
        SCSpatialFeature feature = new SCGeometryFactory().getSpatialFeatureFromFeatureJson(featureString);
        SCKeyTuple decodedTuple = new SCKeyTuple(feature.getId());
        // update feature with decoded values
        feature.setStoreId(decodedTuple.getStoreId());
        feature.setLayerId(decodedTuple.getLayerId());
        feature.setId(decodedTuple.getFeatureId());
        return feature;
    }

    /**
     * Returns an SCSpatialFeature instance based on the message from the bridge to create a new feature.
     **
     * @param payload the JsonNode representing the message payload
     * @return
     * @throws UnsupportedEncodingException
     */
    private SCSpatialFeature getNewFeature(JsonNode payload) throws UnsupportedEncodingException {
      String featureString = payload.get("feature").asText();
      SCSpatialFeature feature = new SCGeometryFactory().getSpatialFeatureFromFeatureJson(featureString);
      feature.setStoreId(payload.get("storeId").asText());
      feature.setLayerId(payload.get("layerId").asText());
      return feature;
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
