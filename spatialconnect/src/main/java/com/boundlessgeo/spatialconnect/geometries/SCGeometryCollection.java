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
import com.boundlessgeo.spatialconnect.scutilities.Json.SCObjectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a GeoJSON FeatureCollection object.
 */
@JsonPropertyOrder({"type", "id", "created", "modified", "features", "properties"})
public class SCGeometryCollection extends SCSpatialFeature
{
    private String type;
    private List<SCSpatialFeature> features;
    @JsonIgnore
    private final String TAG = "SCGeometryCollection";

    public SCGeometryCollection()
    {
        this.type = "FeatureCollection";
        this.features = new ArrayList<>();
    }

    @JsonCreator
    public SCGeometryCollection(List<SCSpatialFeature> geometries)
    {
        this.type = "FeatureCollection";
        this.features = geometries;
    }

    public String getType()
    {
        return this.type;
    }

    public void setType(String type)
    {
        this.type = type;
    }


    public List<SCSpatialFeature> getFeatures()
    {
        return this.features;
    }

    public void setFeatures(List<SCSpatialFeature> features)
    {
        this.features = features;
    }

    public void addTo(SCSpatialFeature scGeometry)
    {
        this.features.add(scGeometry);
        this.setModified(new Date());
    }

    public void delete(SCSpatialFeature scSpatialFeature)
    {
        boolean found = false;
        int idx = 0;
        String searchId = scSpatialFeature.getId();
        for(SCSpatialFeature feature : this.features)
        {
            if(feature.getId().equals(searchId))
            {
                found = true;
                break;
            }
            idx++;
        }
        if(found)
        {
            features.remove(idx);
            this.setModified(new Date());
        }
    }

    public void update(SCSpatialFeature scSpatialFeature)
    {
        boolean found = false;
        int idx = 0;
        String searchId = scSpatialFeature.getId();
        for(SCSpatialFeature feature : this.features)
        {
            if(feature.getId().equals(searchId))
            {
                found = true;
                break;
            }
            idx++;
        }
        if(found)
        {
            features.set(idx, scSpatialFeature);
            this.setModified(new Date());
        }
    }

    public String toJson()
    {
        String geoJson = "";
        try
        {
            geoJson = SCObjectMapper.getMapper().writeValueAsString(this);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in toJson()", ex);
        }
        return geoJson;
    }

    public void toJsonFile(File file)
    {
        try
        {
            SCObjectMapper.getMapper().writeValue(file, this);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in toJsonFile()", ex);
        }
    }
}
