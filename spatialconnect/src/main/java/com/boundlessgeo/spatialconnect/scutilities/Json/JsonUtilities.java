/**
 * Copyright 2015-2016 Boundless, http://boundlessgeo.com
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
 * See the License for the specific language governing permissions and limitations under the
 * License
 */
package com.boundlessgeo.spatialconnect.scutilities.Json;

import android.util.Log;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryFactory;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class JsonUtilities {
  private final String TAG = "JsonUtilities";

  public JsonUtilities() {
  }

  public Map<String, Object> getMapFromJson(String json) {
    Map<String, Object> map = new HashMap<>();
    try {
      map = ObjectMappers.getMapper().readValue(json, new TypeReference<HashMap<String, Object>>() {
      });
    } catch (Exception ex) {
      Log.e(TAG, "Error in getMapFromJson()", ex);
    }
    return map;
  }

  public String getTypeFromJson(String json) {
    String type = "";
    HashMap<String, Object> map = (HashMap<String, Object>) getMapFromJson(json);
    if (map.containsKey("type")) {
      type = map.get("type").toString();
    }
    return type.toLowerCase(Locale.US);
  }

  public SCSpatialFeature getSpatialDataTypeFromJson(String json) {
    String type = getTypeFromJson(json);
    SCGeometryFactory factory = new SCGeometryFactory();
    switch (type) {
      case "point":
        return factory.getPointFromGeoJson(json);
      case "linestring":
        return factory.getLineStringFromGeoJson(json);
      case "polygon":
        return factory.getPolygonFromGeoJson(json);
      case "multipoint":
        return factory.getMultiPointFromGeoJson(json);
      case "multilinestring":
        return factory.getMultiLineStringFromGeoJson(json);
      case "multipolygon":
        return factory.getMultiPolygonFromGeoJson(json);
      case "feature":
        return getSpatialFeatureFromJson(json);
      default:
        return null;
    }
  }

  public SCSpatialFeature getSpatialFeatureFromJson(String json) {
    SCGeometryFactory factory = new SCGeometryFactory();
    SCSpatialFeature feature = new SCSpatialFeature();

    String id = null;
    String created = null;
    String modified = null;
    Map<String, Object> properties = null;

    try {
      ObjectMapper mapper = ObjectMappers.getMapper();
      JsonNode node = mapper.readTree(json);

      JsonNode idNode = node.get("id");
      if (idNode != null) {
        id = idNode.asText();
      }

      JsonNode dateNode = node.get("created");
      if (dateNode != null) {
        created = dateNode.asText();
      }

      dateNode = node.get("modified");
      if (dateNode != null) {
        modified = dateNode.asText();
      }

      JsonNode geometryNode = node.get("geometry");

      if (geometryNode != null) {
        SCGeometry scGeometry;
        String type = geometryNode.get("type").asText();
        String geomJson = geometryNode.toString();

        switch (type.toLowerCase(Locale.US)) {
          case "point":
            scGeometry = factory.getPointFromGeoJson(geomJson);
            break;
          case "linestring":
            scGeometry = factory.getLineStringFromGeoJson(geomJson);
            break;
          case "polygon":
            scGeometry = factory.getPolygonFromGeoJson(geomJson);
            break;
          case "multipoint":
            scGeometry = factory.getMultiPointFromGeoJson(geomJson);
            break;
          case "multilinestring":
            scGeometry = factory.getMultiLineStringFromGeoJson(geomJson);
            break;
          case "multipolygon":
            scGeometry = factory.getMultiPolygonFromGeoJson(geomJson);
            break;
          default:
            return null;
        }
        feature = scGeometry;
      }

      JavaType javaType =
          mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);

      JsonNode propertiesNode = node.get("properties");
      if (propertiesNode != null) {
        properties = mapper.readValue(propertiesNode.traverse(), javaType);
      }

      if (id != null) {
        feature.setId(id);
      }
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      if (created != null) {
        feature.setCreated(formatter.parse(created));
      }
      if (modified != null) {
        feature.setModified(formatter.parse(modified));
      }
      if (properties != null) {
        feature.setProperties(properties);
      }
      JsonNode layerIdNode = node.get("layerId");
      if (layerIdNode != null) {
        feature.setLayerId(layerIdNode.asText());
      }
      JsonNode storeIdNode = node.get("storeId");
      if (storeIdNode != null) {
        feature.setStoreId(storeIdNode.asText());
      }
    } catch (Exception ex) {
      //Log.e(TAG, "Error in getFeatureFromGeoJson(String)", ex);
      ex.printStackTrace();
    }

    return feature;
  }
}
