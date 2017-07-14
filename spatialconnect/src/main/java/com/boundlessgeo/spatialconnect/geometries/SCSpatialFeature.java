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
package com.boundlessgeo.spatialconnect.geometries;


import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.scutilities.Json.SCObjectMapper;
import com.boundlessgeo.spatialconnect.stores.SCKeyTuple;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SCSpatialFeature
{
    /**
     * For a GeoPackage, the ID will follow the convention storeId.featureTableName.idOfRow
     */
    private static final String LOG_TAG = SCSpatialFeature.class.getSimpleName();
    protected String storeId;
    protected String layerId;
    protected String id;
    protected Date created;
    protected Date modified;
    protected Map<String, Object> properties;
    protected Map<String, Object> metadata;

    public SCSpatialFeature() {
        this.created = new Date();
        this.properties = new HashMap<>();
        this.metadata = new HashMap<>();
        this.metadata.put("layerId", layerId);
        this.metadata.put("storeId", storeId);
    }

//    public SCSpatialFeature(HashMap<String, Object> jsonMap) {
//        // TODO
//
//        SCGeometryFactory factory = new SCGeometryFactory();
//        SCSpatialFeature feature = new SCSpatialFeature();
//
//        String id = null;
//        String created = null;
//        String modified = null;
//        Map<String, Object> properties = null;
//
//        try
//        {
//            ObjectMapper mapper = SCObjectMapper.getMapper();
//            JsonNode node = mapper.readTree(json);
//
//            JsonNode idNode = node.get("id");
//            if(idNode != null)
//            {
//                id = idNode.asText();
//            }
//
//            JsonNode dateNode = node.get("created");
//            if (dateNode != null)
//            {
//                created = dateNode.asText();
//            }
//
//            dateNode = node.get("modified");
//            if (dateNode != null)
//            {
//                modified = dateNode.asText();
//            }
//
//            JsonNode geometryNode = node.get("geometry");
//
//            if (geometryNode != null) {
//                SCGeometry scGeometry;
//                String type = geometryNode.get("type").asText();
//                String geomJson = geometryNode.toString();
//
//                switch (type.toLowerCase(Locale.US))
//                {
//                    case "point":
//                        scGeometry = factory.getPointFromGeoJson(geomJson);
//                        break;
//                    case "linestring":
//                        scGeometry = factory.getLineStringFromGeoJson(geomJson);
//                        break;
//                    case "polygon":
//                        scGeometry = factory.getPolygonFromGeoJson(geomJson);
//                        break;
//                    case "multipoint":
//                        scGeometry = factory.getMultiPointFromGeoJson(geomJson);
//                        break;
//                    case "multilinestring":
//                        scGeometry = factory.getMultiLineStringFromGeoJson(geomJson);
//                        break;
//                    case "multipolygon":
//                        scGeometry = factory.getMultiPolygonFromGeoJson(geomJson);
//                        break;
//                    default:
//                        return null;
//                }
//                feature = scGeometry;
//            }
//
//            JavaType javaType = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
//
//            JsonNode propertiesNode = node.get("properties");
//            if (propertiesNode != null)
//            {
//                properties = mapper.readValue(propertiesNode.traverse(), javaType);
//            }
//
//            if(id != null)
//            {
//                feature.setId(id);
//            }
//            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//            if(created != null)
//            {
//                feature.setCreated(formatter.parse(created));
//            }
//            if(modified != null)
//            {
//                feature.setModified(formatter.parse(modified));
//            }
//            if(properties != null)
//            {
//                feature.setProperties(properties);
//            }
//            JsonNode layerIdNode = node.get("layerId");
//            if(layerIdNode != null)
//            {
//                feature.setLayerId(layerIdNode.asText());
//            }
//            JsonNode storeIdNode = node.get("storeId");
//            if(storeIdNode != null)
//            {
//                feature.setStoreId(storeIdNode.asText());
//            }
//        }
//        catch (Exception ex)
//        {
//            //Log.e(TAG, "Error in getFeatureFromGeoJson(String)", ex);
//            ex.printStackTrace();
//        }
//
//        return feature;
//    }

    public String getId()
    {
        if(this.id == null || this.id.isEmpty())
        {
            this.id = UUID.randomUUID().toString();
        }
        return this.id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss", timezone="GMT")
    public Date getCreated()
    {
        return this.created;
    }

    public void setCreated(Date date)
    {
        this.created = date;
    }

    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss", timezone="GMT")
    public Date getModified()
    {
        return this.modified;
    }

    public void setModified(Date modified)
    {
        this.modified = modified;
    }


    public Map<String, Object> getProperties()
    {
        return this.properties;
    }


    public void setProperties(Map<String, Object> properties)
    {
        this.properties = properties;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreId() { return this.storeId; }

    public void setLayerId(String layerId) {
        this.layerId = layerId;
    }

    public String getLayerId() {
        return this.layerId;
    }

    public SCKeyTuple getKey() {
        return new SCKeyTuple(this.storeId, this.layerId, this.id);
    }

    public Map<String, Object> getMetadata() {
        Map map = new HashMap<>(3);
        map.put("storeId", storeId);
        map.put("layerId", layerId);
        map.put("client", SpatialConnect.getInstance().getDeviceIdentifier());
        return map;
    }

    public String toJson() {
        try {
            return SCObjectMapper.getMapper().writeValueAsString(this);
        }
        catch (JsonProcessingException e) {
            Log.e(LOG_TAG, "error converting to json: " + e.getMessage());
        }
        return null;
    }

    public  Map<String, Object> toMap() {
        Map<String, Object> map = null;
        try {
            map = SCObjectMapper.getMapper().convertValue(this,  Map.class);
            map.put("type", "Feature");
        } catch (Exception e) {
            Log.e(LOG_TAG, "error converting to map: " + e.getMessage());
        }

        return map;
    }

//    public HashMap<String, Object> toJSON() {
//        HashMap<String, Object> json = new HashMap<>();
//
//        // TODO
//
//        return json;
//    }
}
