package com.boundlessgeo.spatialconnect.geometries;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class SCPoint extends SCGeometry
{
    public SCPoint(Geometry point)
    {
        super(point);
    }

    @JsonIgnore
    public Point getJTS()
    {
        return (Point)this.geometry;
    }

    @Override
    public String toString()
    {
        return geometry.toText();
    }
}
