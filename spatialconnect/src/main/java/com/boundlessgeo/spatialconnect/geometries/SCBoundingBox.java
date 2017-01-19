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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vividsolutions.jts.geom.Envelope;

import java.io.File;

public class SCBoundingBox
{
    private Double[] bbox = new Double[4];

    private final String TAG = "SCBoundingBox";

    public SCBoundingBox(double llx, double lly, double urx, double ury)
    {
        bbox[0] = llx;
        bbox[1] = lly;
        bbox[2] = urx;
        bbox[3] = ury;
    }

    public SCBoundingBox(SCGeometry scGeometry)
    {
        Envelope envelope = scGeometry.geometry.getEnvelopeInternal();
        bbox[0] = envelope.getMinX();
        bbox[1] = envelope.getMinY();
        bbox[2] = envelope.getMaxX();
        bbox[3] = envelope.getMaxY();
    }

    @JsonProperty("bbox")
    public Double[] getBbox()
    {
        return bbox;
    }

    public void setBbox(Double[] bbox)
    {
        this.bbox = bbox;
    }

    private boolean checkSize(Double[] bbox)
    {
        if(bbox.length != 4) return false;
        return true;
    }

    public String toGeoJson()
    {
        String geoJson = "";
        try
        {
            geoJson = SCObjectMapper.getMapper().writeValueAsString(this);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in toGeoJson()", ex);
        }
        return geoJson;
    }

    public void toGeoJsonFile(File file)
    {
        try
        {
            SCObjectMapper.getMapper().writeValue(file, this);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in toGeoJsonFile()", ex);
        }
    }

    public Double getMinX() {
        return bbox[0];
    }

    public Double getMinY() {
        return bbox[1];
    }

    public Double getMaxX() {
        return bbox[2];
    }

    public Double getMaxY() {
        return bbox[3];
    }
}
