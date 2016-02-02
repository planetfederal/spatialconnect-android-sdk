package com.boundlessgeo.spatialconnect.geometries;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;

public class SCLinearRing extends SCGeometry
{
    public SCLinearRing(Geometry geometry)
    {
        super(geometry);
    }

    @JsonIgnore
    public LinearRing getJTS()
    {
        return (LinearRing)this.geometry;
    }

    @Override
    public String toString()
    {
        return geometry.toString();
    }
}
