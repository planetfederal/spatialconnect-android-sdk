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
import com.boundlessgeo.spatialconnect.scutilities.Json.JsonUtilities;
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

/**
 * SCSpatialFeature is the primary domain object of SpatialConnect and you can think of it as
 * a row in a database.
 * The word "spatial" in this context does not imply a geometry, instead it is meant to be thought
 * of as a dimension in some n-dimensional space.  If a SCSpatialFeature has a geometry, then it
 * should be a {@link SCGeometry}.
 */
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

    public HashMap<String, Object> toMap() {
        HashMap<String, Object> map = null;
        try {
            map = SCObjectMapper.getMapper().convertValue(this,  HashMap.class);
            map.put("type", "Feature");
        } catch (Exception e) {
            Log.e(LOG_TAG, "error converting to map: " + e.getMessage());
        }

        return map;
    }

    public HashMap<String, Object> toJSON() {
        return toMap();

        // TODO eventually should change this to write to a HashMap on it's own, without using objectmapper
    }
}
