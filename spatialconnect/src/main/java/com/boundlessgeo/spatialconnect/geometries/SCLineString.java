package com.boundlessgeo.spatialconnect.geometries;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class SCLineString extends SCGeometry
{
    public SCLineString(Geometry lineString)
    {
        super(lineString);
    }

    @JsonIgnore
    public LineString getJTS()
    {
        return (LineString)this.geometry;
    }

    @Override
    public String toString()
    {
        return geometry.toText();
    }
}
