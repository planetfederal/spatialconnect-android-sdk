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
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.geometries;


import android.util.Log;

import com.boundlessgeo.spatialconnect.scutilities.Json.ObjectMappers;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import java.io.File;


@JsonPropertyOrder({"type", "id", "created", "modified", "bbox", "geometry", "properties"})
 public class SCGeometry extends SCSpatialFeature
{


    /**
     * Geometry type enumeration.
     */
    public enum Type {

        POINT(Point.class),
        LINESTRING(LineString.class),
        POLYGON(Polygon.class),
        MULTIPOINT(MultiPoint.class),
        MULTILINESTRING(MultiLineString.class),
        MULTIPOLYGON(MultiPolygon.class),
        GEOMETRYCOLLECTION(GeometryCollection.class),
        LINEARRING(LinearRing.class);


        private final Class<? extends Geometry> type;
        private final String name;

        Type(Class<? extends Geometry> type) {
            this.type = type;
            this.name = type.getSimpleName();
        }

        public static Type from(Geometry geom) {
            for (Type gt : Type.values()) {
                if (gt.type == geom.getClass()) {
                    return gt;
                }
            }
            return null;
        }

    }

    protected Type geometryType;
    protected Geometry geometry;
    protected String exportType;
    protected String jtsGeometryType;
    protected SCBoundingBox bbox;
    protected String geometryGeoJson;
    private final String TAG = "SCGeometry";

    @JsonCreator
    public SCGeometry(Geometry geometry)
    {
        super();
        this.exportType = "Feature";
        this.geometry = geometry;
        this.jtsGeometryType = this.geometry.getGeometryType();
        this.bbox = new SCBoundingBox(this);
        this.geometryGeoJson = getGeometryGeoJson(this.geometry);
    }

    @JsonProperty("type")
    public String getExportType()
    {
        return this.exportType;
    }

    @JsonProperty("bbox")
    public Double[] getBbox()
    {
        if(bbox != null)
        {
            return bbox.getBbox();
        }
        return null;
    }

    /*public void setBbox(SCBoundingBox bbox)
    {
        this.bbox = bbox;
    }
    */

    @JsonIgnore()
    public Geometry getGeometry()
    {
        return geometry;
    }
    public void setGeometry(Geometry geometry)
    {
        this.geometry = geometry;
    }

    @JsonRawValue()
    @JsonProperty("geometry")
    public String getGeometryGeoJson()
    {
        return geometryGeoJson;
    }

    public String toJson()
    {
        String json = "";
        try
        {
            json = ObjectMappers.getMapper().writeValueAsString(this);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in toJson()", ex);
        }
        return json;
    }

    @JsonIgnore
    public String getGeometryGeoJson(Geometry geometry)
    {
        String geoJson = "";
        try
        {
            geoJson = ObjectMappers.getMapper().writeValueAsString(geometry);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getGetGeometryGeoJson()", ex);
        }
        return geoJson;
    }

    public void toJsonFile(File file)
    {
        try
        {
            ObjectMappers.getMapper().writeValue(file, this);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in toGeoJsonFile()", ex);
        }
    }

}
