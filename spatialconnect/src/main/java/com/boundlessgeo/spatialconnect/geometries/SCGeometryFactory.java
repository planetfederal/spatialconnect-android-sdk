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
import com.boundlessgeo.spatialconnect.scutilities.Json.JsonUtilities;
import com.boundlessgeo.spatialconnect.scutilities.Json.ObjectMappers;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKTReader;

import java.text.SimpleDateFormat;
import java.util.*;

//TODO - Replace exception handling with logcat when ready for test/deployment in emulator or device
//TODO - Generate JavaDoc

public class SCGeometryFactory
{
    private GeometryFactory geometryFactory;
    private int srid;
    private final String TAG = "SCGeometryFactory";

    public SCGeometryFactory()
    {
        this(0);
    }

    public SCGeometryFactory(int srid)
    {
        this.srid = srid;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), srid);
    }

    //region Point
    public SCPoint getPoint(double x, double y)
    {
        Geometry jtsGeometry = null;
        SCPoint scPoint = null;
        try
        {
            Coordinate coordinate = new Coordinate(x, y);
            jtsGeometry = geometryFactory.createPoint(coordinate);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getPoint(double, double)", ex);
        }
        if (jtsGeometry != null)
        {
            scPoint = new SCPoint(jtsGeometry);
        }

        return scPoint;
    }

    public SCPoint getPoint(double x, double y, double z)
    {
        Geometry jtsGeometry = null;
        SCPoint scPoint = null;
        try
        {
            Coordinate coordinate = new Coordinate(x, y, z);
            jtsGeometry = geometryFactory.createPoint(coordinate);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getPoint(double, double, double)", ex);
        }
        if (jtsGeometry != null)
        {
            scPoint = new SCPoint(jtsGeometry);
        }

        return scPoint;
    }

    public SCPoint getPoint(Coordinate coordinate)
    {
        Geometry jtsGeometry = null;
        SCPoint scPoint = null;
        try
        {
            jtsGeometry = geometryFactory.createPoint(coordinate);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getPoint(Coordinate)", ex);
        }
        if (jtsGeometry != null)
        {
            scPoint = new SCPoint(jtsGeometry);
        }

        return scPoint;
    }

    public SCPoint getPointFromWKT(String wkt)
    {
        if (!wkt.trim().startsWith("POINT")) return null;

        Geometry jtsGeometry = null;
        SCPoint scPoint = null;
        WKTReader reader = new WKTReader(this.geometryFactory);
        try
        {
            jtsGeometry = reader.read(wkt);
        }
        catch (ParseException ex)
        {
            Log.e(TAG, "Error in getPointFromWKT(String)", ex);
        }
        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scPoint = new SCPoint(jtsGeometry);
        }

        return scPoint;
    }

    public SCPoint getPointFromWKB(byte[] wkb)
    {
        Geometry jtsGeometry = null;
        SCPoint scPoint = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(wkb);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getPointFromWKB(byte[])", ex);
        }
        if (jtsGeometry != null)
        {
            scPoint = new SCPoint(jtsGeometry);
        }
        return scPoint;
    }

    public SCPoint getPointFromWKB(String hex)
    {
        SCPoint scPoint = null;
        Geometry jtsGeometry = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(hex);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getPointFromWKB(String)", ex);
        }
        if (jtsGeometry != null)
        {
            scPoint = new SCPoint(jtsGeometry);
        }
        return scPoint;
    }

    public SCPoint getPointFromGeoJson(String json)
    {
        SCPoint scPoint = null;
        Geometry jtsGeometry = getGeometryFromJson(json);

        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scPoint = new SCPoint(jtsGeometry);
        }
        return scPoint;
    }
    //endregion

    //region LineString
    public SCLineString getLineString(List<SCPoint> scPoints)
    {
        Coordinate[] coordinates = new Coordinate[scPoints.size()];
        int i = 0;
        for (SCPoint scPoint : scPoints)
        {
            coordinates[i] = scPoint.getJTS().getCoordinate();
            i++;
        }
        return getLineString(coordinates);
    }

    public SCLineString getLineString(Coordinate[] coordinates)
    {
        Geometry jtsGeometry = null;
        SCLineString scLineString = null;
        try
        {
            jtsGeometry = this.geometryFactory.createLineString(coordinates);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getLineString()", ex);
        }
        if (jtsGeometry != null)
        {
            scLineString = new SCLineString(jtsGeometry);
        }
        return scLineString;
    }

    public SCLineString getLineStringFromWKT(String wkt)
    {
        if (!wkt.trim().startsWith("LINESTRING")) return null;

        WKTReader reader = new WKTReader(this.geometryFactory);
        Geometry jtsGeometry = null;
        SCLineString scLineString = null;
        try
        {
            jtsGeometry = reader.read(wkt);
        }
        catch (ParseException ex)
        {
            Log.e(TAG, "Error in getLineStringFromWKT(String)", ex);
        }
        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scLineString = new SCLineString(jtsGeometry);
        }
        return scLineString;
    }

    public SCLineString getLineStringFromWKB(byte[] wkb)
    {
        Geometry jtsGeometry = null;
        SCLineString scLineString = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(wkb);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getLineStringFromWKB(byte[])", ex);
        }
        if (jtsGeometry != null)
        {
            scLineString = new SCLineString(jtsGeometry);
        }
        return scLineString;
    }

    public SCLineString getLineStringFromWKB(String hex)
    {
        SCLineString scLineString = null;
        Geometry jtsGeometry = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(hex);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getLineStringFromWKB(String)", ex);
        }
        if (jtsGeometry != null)
        {
            scLineString = new SCLineString(jtsGeometry);
        }
        return scLineString;
    }

    public SCLineString getLineStringFromGeoJson(String json)
    {
        SCLineString scLineString = null;
        Geometry jtsGeometry = getGeometryFromJson(json);

        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scLineString = new SCLineString(jtsGeometry);
        }
        return scLineString;
    }
    //endregion

    //region LinearRing
    public SCLinearRing getLinearRing(List<SCPoint> points)
    {
        Coordinate[] coordinates = new Coordinate[points.size()];
        int i = 0;
        for (SCPoint point : points)
        {
            coordinates[i] = point.getJTS().getCoordinate();
            i++;
        }
        return getLinearRing(coordinates);
    }

    public SCLinearRing getLinearRing(SCPoint[] points)
    {
        Coordinate[] coordinates = new Coordinate[points.length];
        for (int i = 0; i < points.length; i++)
        {
            coordinates[i] = points[i].getJTS().getCoordinate();
        }
        return getLinearRing(coordinates);
    }

    public SCLinearRing getLinearRing(Coordinate[] coordinates)
    {
        Geometry jtsGeometry = null;
        SCLinearRing scLinearRing = null;
        try
        {
            jtsGeometry = this.geometryFactory.createLinearRing(coordinates);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getLinearRing()", ex);
        }
        if (jtsGeometry != null)
        {
            scLinearRing = new SCLinearRing(jtsGeometry);
        }
        return scLinearRing;
    }

    public SCLinearRing getLinearRingFromWKT(String wkt)
    {
        if (!wkt.trim().startsWith("LINEARRING")) return null;

        Geometry jtsGeometry = null;
        SCLinearRing scLinearRing = null;
        WKTReader reader = new WKTReader(this.geometryFactory);
        try
        {
            jtsGeometry = reader.read(wkt);
        }
        catch (ParseException ex)
        {
            Log.e(TAG, "Error in getLinearRingFromWKT()", ex);
        }
        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scLinearRing = new SCLinearRing(jtsGeometry);
        }
        return scLinearRing;
    }

    public SCLinearRing getLinearRingFromWKB(byte[] wkb)
    {
        Geometry jtsGeometry = null;
        SCLinearRing scLinearRing = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(wkb);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getLinearRingFromWKB(byte[])", ex);
        }
        if (jtsGeometry != null)
        {
            scLinearRing = new SCLinearRing(jtsGeometry);
        }
        return scLinearRing;
    }

    public SCLinearRing getLinearRingFromWKB(String hex)
    {
        SCLinearRing scLinearRing = null;
        Geometry jtsGeometry = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(hex);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getLinearRingFromWKB(String)", ex);
        }
        if (jtsGeometry != null)
        {
            scLinearRing = new SCLinearRing(jtsGeometry);
        }
        return scLinearRing;
    }
    //endregion

    //region MultiPoint
    public SCMultiPoint getMultiPoint(List<SCPoint> points)
    {
        Point[] jtsPoints = new Point[points.size()];
        Geometry jtsGeometry = null;
        SCMultiPoint scMultiPoint = null;

        try
        {
            int i = 0;
            for (SCPoint scPoint : points)
            {
                jtsPoints[i] = scPoint.getJTS();
                i++;
            }

            jtsGeometry = this.geometryFactory.createMultiPoint(jtsPoints);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getMultiPoint()", ex);
        }
        if (jtsGeometry != null)
        {
            scMultiPoint = new SCMultiPoint(jtsGeometry);
        }
        return scMultiPoint;
    }

    public SCMultiPoint getMultiPoint(SCPoint[] scPoints)
    {
        Point[] jtsPoints = new Point[scPoints.length];
        SCMultiPoint scMultiPoint = null;
        Geometry jtsGeometry = null;

        try
        {
            for (int i = 0; i < scPoints.length; i++)
            {
                jtsPoints[i] = scPoints[i].getJTS();
            }

            jtsGeometry = this.geometryFactory.createMultiPoint(jtsPoints);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getMultiPoint()", ex);
        }
        if (jtsGeometry != null)
        {
            scMultiPoint = new SCMultiPoint(jtsGeometry);
        }
        return scMultiPoint;
    }


    public SCMultiPoint getMultiPointFromWKT(String wkt)
    {
        if (!wkt.trim().startsWith("MULTIPOINT")) return null;

        SCMultiPoint scMultiPoint = null;
        Geometry jtsGeometry = null;
        WKTReader reader = new WKTReader(this.geometryFactory);
        try
        {
            jtsGeometry = reader.read(wkt);
        }
        catch (ParseException ex)
        {
            Log.e(TAG, "Error in getMultiPointFromWKT()", ex);
        }
        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scMultiPoint = new SCMultiPoint(jtsGeometry);
        }
        return scMultiPoint;
    }

    public SCMultiPoint getMultiPointFromWKB(byte[] wkb)
    {
        Geometry jtsGeometry = null;
        SCMultiPoint scMultiPoint = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(wkb);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getMultiPointFromWKB(byte[])", ex);
        }
        if (jtsGeometry != null)
        {
            scMultiPoint = new SCMultiPoint(jtsGeometry);
        }
        return scMultiPoint;
    }

    public SCMultiPoint getMultiPointFromWKB(String hex)
    {
        SCMultiPoint scMultiPoint = null;
        Geometry jtsGeometry = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(hex);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getMultiPointFromWKB(String)", ex);
        }
        if (jtsGeometry != null)
        {
            scMultiPoint = new SCMultiPoint(jtsGeometry);
        }
        return scMultiPoint;
    }

    public SCMultiPoint getMultiPointFromGeoJson(String json)
    {
        SCMultiPoint scMultiPoint = null;
        Geometry jtsGeometry = getGeometryFromJson(json);

        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scMultiPoint = new SCMultiPoint(jtsGeometry);
        }
        return scMultiPoint;
    }
    //endregion

    //region MultiLineString
    public SCMultiLineString getMultiLineString(List<SCLineString> lineStrings)
    {
        LineString[] jtsLineStrings = new LineString[lineStrings.size()];
        SCMultiLineString scMultiLineString = null;
        Geometry jtsGeometry = null;

        try
        {
            int i = 0;
            for (SCLineString lineString : lineStrings)
            {
                jtsLineStrings[i] = lineString.getJTS();
                i++;
            }

            jtsGeometry = this.geometryFactory.createMultiLineString(jtsLineStrings);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getMultiLineString()", ex);
        }
        if (jtsGeometry != null)
        {
            scMultiLineString = new SCMultiLineString(jtsGeometry);
        }
        return scMultiLineString;
    }

    public SCMultiLineString getMultiLineString(SCLineString[] scLineStrings)
    {
        LineString[] jtsLineStrings = new LineString[scLineStrings.length];
        SCMultiLineString scMultiLineString = null;
        Geometry jtsGeometry = null;

        try
        {
            for (int i = 0; i < scLineStrings.length; i++)
            {
                jtsLineStrings[i] = scLineStrings[i].getJTS();
            }

            jtsGeometry = this.geometryFactory.createMultiLineString(jtsLineStrings);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getMultiLineString()", ex);
        }
        if (jtsGeometry != null)
        {
            scMultiLineString = new SCMultiLineString(jtsGeometry);
        }
        return scMultiLineString;
    }

    public SCMultiLineString getMultiLineStringFromWKT(String wkt)
    {
        if (!wkt.trim().startsWith("MULTILINESTRING")) return null;

        Geometry jtsGeometry = null;
        SCMultiLineString scMultiLineString = null;
        WKTReader reader = new WKTReader(this.geometryFactory);

        try
        {
            jtsGeometry = reader.read(wkt);
        }
        catch (ParseException ex)
        {
            Log.e(TAG, "Error in getMultiLineStringFromWKT()", ex);
        }
        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scMultiLineString = new SCMultiLineString(jtsGeometry);
        }
        return scMultiLineString;
    }

    public SCMultiLineString getMultiLineStringFromWKB(byte[] wkb)
    {
        Geometry jtsGeometry = null;
        SCMultiLineString scMultiLineString = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(wkb);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getMultiLineStringFromWKB(byte[])", ex);
        }
        if (jtsGeometry != null)
        {
            scMultiLineString = new SCMultiLineString(jtsGeometry);
        }
        return scMultiLineString;
    }

    public SCMultiLineString getMultiLineStringFromWKB(String hex)
    {
        SCMultiLineString scMultiLineString = null;
        Geometry jtsGeometry = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(hex);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getMultiLineStringFromWKB(String)", ex);
        }
        if (jtsGeometry != null)
        {
            scMultiLineString = new SCMultiLineString(jtsGeometry);
        }
        return scMultiLineString;
    }

    public SCMultiLineString getMultiLineStringFromGeoJson(String json)
    {
        SCMultiLineString scMultiLineString = null;
        Geometry jtsGeometry = getGeometryFromJson(json);

        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scMultiLineString = new SCMultiLineString(jtsGeometry);
        }
        return scMultiLineString;
    }
    //endregion

    //region Polygon
    public SCPolygon getPolygon(SCLinearRing shell)
    {
        SCPolygon scPolygon = null;
        Geometry jtsGeometry = null;

        try
        {
            jtsGeometry = this.geometryFactory.createPolygon(shell.getJTS(), null);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getPolygon()", ex);
        }
        if (jtsGeometry != null)
        {
            scPolygon = new SCPolygon(jtsGeometry);
        }
        return scPolygon;
    }

    public SCPolygon getPolygon(SCLinearRing shell, List<SCLinearRing> holes)
    {
        LinearRing[] jtsLinearRings = new LinearRing[holes.size()];
        SCPolygon scPolygon = null;
        Geometry jtsGeometry = null;

        try
        {
            int i = 0;
            for (SCLinearRing scLinearRing : holes)
            {
                jtsLinearRings[i] = scLinearRing.getJTS();
                i++;
            }
            jtsGeometry = this.geometryFactory.createPolygon(shell.getJTS(), jtsLinearRings);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getPolygon()", ex);
        }
        if (jtsGeometry != null)
        {
            scPolygon = new SCPolygon(jtsGeometry);
        }
        return scPolygon;
    }

    public SCPolygon getPolygonFromWKT(String wkt)
    {
        if (!wkt.trim().startsWith("POLYGON")) return null;

        SCPolygon scPolygon = null;
        Geometry jtsGeometry = null;
        WKTReader reader = new WKTReader(this.geometryFactory);

        try
        {
            jtsGeometry = reader.read(wkt);
        }
        catch (ParseException ex)
        {
            Log.e(TAG, "Error in getPolygonFromWKT()", ex);
        }
        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scPolygon = new SCPolygon(jtsGeometry);
        }
        return scPolygon;
    }

    public SCPolygon getPolygonFromWKB(byte[] wkb)
    {
        Geometry jtsGeometry = null;
        SCPolygon scPolygon = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(wkb);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getPolygonFromWKB(byte[])", ex);
        }
        if (jtsGeometry != null)
        {
            scPolygon = new SCPolygon(jtsGeometry);
        }
        return scPolygon;
    }

    public SCPolygon getPolygonFromWKB(String hex)
    {
        SCPolygon scPolygon = null;
        Geometry jtsGeometry = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(hex);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getPolygonFromWKB(String)", ex);
        }
        if (jtsGeometry != null)
        {
            scPolygon = new SCPolygon(jtsGeometry);
        }
        return scPolygon;
    }

    public SCPolygon getPolygonFromGeoJson(String json)
    {
        SCPolygon scPolygon = null;
        Geometry jtsGeometry = getGeometryFromJson(json);

        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scPolygon = new SCPolygon(jtsGeometry);
        }
        return scPolygon;
    }
    //endregion Polygon

    //region MultiPolygon
    public SCMultiPolygon getMultiPolygon(List<SCPolygon> polygons)
    {
        Polygon[] jtsPolygons = new Polygon[polygons.size()];
        SCMultiPolygon scMultiPolygon = null;
        Geometry jtsGeometry = null;

        try
        {
            int i = 0;
            for (SCPolygon polygon : polygons)
            {
                jtsPolygons[i] = polygon.getJTS();
                i++;
            }
            jtsGeometry = this.geometryFactory.createMultiPolygon(jtsPolygons);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getMultiPolygon()", ex);
        }
        if (jtsGeometry != null)
        {
            scMultiPolygon = new SCMultiPolygon(jtsGeometry);
        }
        return scMultiPolygon;
    }

    public SCMultiPolygon getMultiPolygonFromWKT(String wkt)
    {
        if (!wkt.trim().startsWith("MULTIPOLYGON")) return null;

        SCMultiPolygon scMultiPolygon = null;
        Geometry jtsGeometry = null;
        WKTReader reader = new WKTReader(this.geometryFactory);
        try
        {
            jtsGeometry = reader.read(wkt);
        }
        catch (ParseException ex)
        {
            Log.e(TAG, "Error in getMultiPolygonFromWKT()", ex);
        }
        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scMultiPolygon = new SCMultiPolygon(jtsGeometry);
        }
        return scMultiPolygon;
    }

    public SCMultiPolygon getMultiPolygonFromWKB(byte[] wkb)
    {
        Geometry jtsGeometry = null;
        SCMultiPolygon scMultiPolygon = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(wkb);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getMultiPolygonFromWKB(byte[])", ex);
        }
        if (jtsGeometry != null)
        {
            scMultiPolygon = new SCMultiPolygon(jtsGeometry);
        }
        return scMultiPolygon;
    }

    public SCMultiPolygon getMultiPolygonFromWKB(String hex)
    {
        SCMultiPolygon scMultiPolygon = null;
        Geometry jtsGeometry = null;

        try
        {
            jtsGeometry = getGeometryFromWKB(hex);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getMultiPolygonFromWKB(String)", ex);
        }
        if (jtsGeometry != null)
        {
            scMultiPolygon = new SCMultiPolygon(jtsGeometry);
        }
        return scMultiPolygon;
    }

    public SCMultiPolygon getMultiPolygonFromGeoJson(String json)
    {
        SCMultiPolygon scMultiPolygon = null;
        Geometry jtsGeometry = getGeometryFromJson(json);

        if (jtsGeometry != null)
        {
            jtsGeometry.setSRID(this.srid);
            scMultiPolygon = new SCMultiPolygon(jtsGeometry);
        }
        return scMultiPolygon;
    }
    //endregion MultiPolygon

    public SCSpatialFeature getSpatialFeatureFromFeatureJson(String json)
    {
        JsonUtilities utilities = new JsonUtilities();
        return utilities.getSpatialFeatureFromJson(json);
    }

    public SCGeometryCollection getGeometryCollectionFromGeometryCollectionJson(String json)
    {
        JsonUtilities utilities = new JsonUtilities();
        SCGeometryCollection scGeometryCollection = null;
        try
        {
            JsonNode node = ObjectMappers.getMapper().readTree(json);
            Iterator<JsonNode> it = node.get("geometries").iterator();
            List<SCSpatialFeature> features = new ArrayList<>();
            while (it.hasNext())
            {
                JsonNode jFeature = it.next();
                String featureJson = jFeature.toString();
                features.add(utilities.getSpatialDataTypeFromJson(featureJson));
            }
            scGeometryCollection = new SCGeometryCollection(features);
        }
        catch(Exception ex)
        {
            Log.e(TAG, "Error in getGeometryCollectionFromGeometryCollectionJson(String)", ex);
        }
        return scGeometryCollection;
    }

    public SCGeometryCollection getGeometryCollectionFromFeatureCollectionJson(String json)
    {
        JsonUtilities utilities = new JsonUtilities();
        SCGeometryCollection scGeometryCollection = null;
        String id = null;
        String created = null;
        String modified = null;
        Map<String, Object> properties = null;

        try
        {
            ObjectMapper mapper = ObjectMappers.getMapper();
            JsonNode node = mapper.readTree(json);
            JsonNode idNode = node.get("id");
            if(idNode != null)
            {
                id = idNode.asText();
            }

            JsonNode dateNode = node.get("created");
            if (dateNode != null)
            {
                created = dateNode.asText();
            }

            dateNode = node.get("modified");
            if (dateNode != null)
            {
                modified = dateNode.asText();
            }

            JsonNode propertiesNode = node.get("properties");
            if (propertiesNode != null)
            {
                JavaType javaType = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
                properties = mapper.readValue(propertiesNode.traverse(), javaType);
            }

            Iterator<JsonNode> it = node.get("features").iterator();
            List<SCSpatialFeature> features = new ArrayList<>();
            while (it.hasNext())
            {
                JsonNode jFeature = it.next();
                String featureJson = jFeature.toString();
                features.add(utilities.getSpatialFeatureFromJson(featureJson));
            }
            scGeometryCollection = new SCGeometryCollection(features);

            if(id != null)
            {
                scGeometryCollection.setId(id);
            }
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            if(created != null)
            {
                scGeometryCollection.setCreated(formatter.parse(created));
            }
            if(modified != null)
            {
                scGeometryCollection.setModified(formatter.parse(modified));
            }
            if(properties != null)
            {
                scGeometryCollection.setProperties(properties);
            }
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in getGeometryCollectionFromFeatureCollectionJson(String)", ex);
        }

        return scGeometryCollection;
    }

    //region Miscellaneous
    public void setSRID(int srid)
    {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), srid);
    }

    public Geometry getGeometryFromJson(String json)
    {
        Geometry geometry = null;
        try
        {
            geometry = ObjectMappers.getMapper().readValue(json, Geometry.class);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Error in get geometryFromGeoJson(String). Check the syntax: " + json, ex);
        }
        if (geometry != null)
        {
            geometry.setSRID(this.srid);
        }
        return geometry;
    }

    public Geometry getGeometryFromWKB(byte[] wkb) throws ParseException
    {
        Geometry geometry;
        WKBReader wkbReader = new WKBReader(this.geometryFactory);

        geometry = wkbReader.read(wkb);

        return geometry;
    }

    public Geometry getGeometryFromWKB(String hex) throws ParseException
    {
        return getGeometryFromWKB(WKBReader.hexToBytes(hex));
    }
    //endregion
}
