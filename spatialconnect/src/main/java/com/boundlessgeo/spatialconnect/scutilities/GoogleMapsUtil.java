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
package com.boundlessgeo.spatialconnect.scutilities;

import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.stores.SCKeyTuple;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import io.jeo.geom.Geom;


/**
 * Utility class for converting JTS geometry objects into GoogleMaps objects and adding them to a GoogleMap.
 */
public class GoogleMapsUtil {

    public GoogleMapsUtil() {

    }

    public static void addToMap(GoogleMap gmap, SCGeometry geomFeature) {
        switch (SCGeometry.Type.from(geomFeature.getGeometry())) {
            case POINT:
                addPointToMap(gmap, geomFeature);
                break;

            case MULTIPOINT:
                MultiPoint multiPoint = (MultiPoint) geomFeature.getGeometry();
                for (Point point : Geom.iterate(multiPoint)) {
                    addPointToMap(gmap, point, geomFeature.getKey());
                }
                break;

            case POLYGON:
                addPolygonToMap(gmap, geomFeature);
                break;


            case MULTIPOLYGON:
                MultiPolygon multiPolygon = (MultiPolygon) geomFeature.getGeometry();
                for (Polygon polygon : Geom.iterate(multiPolygon)) {
                    addPolygonToMap(gmap, polygon);
                }
                break;
        }

    }

    private static void addPolygonToMap(GoogleMap gmap, SCGeometry feature) {
        addPolygonToMap(gmap, (Polygon) feature.getGeometry());
    }


    private static void addPolygonToMap(GoogleMap gmap, Polygon polygon) {
        PolygonOptions opts = new PolygonOptions();
        for (Coordinate c : polygon.getExteriorRing().getCoordinates()) {
            opts.add(new LatLng(c.y, c.x));
        }
        gmap.addPolygon(opts);
    }

    public static Marker addPointToMap(GoogleMap gmap, SCGeometry feature) {
        return addPointToMap(gmap, (Point) feature.getGeometry(), feature.getKey());
    }

    public static Marker addPointToMap(GoogleMap gmap, Point point, SCKeyTuple tuple) {
        LatLng latLng = new LatLng(point.getY(), point.getX());
        MarkerOptions mo = new MarkerOptions();
        mo.position(latLng);
        mo.title(tuple.getLayerId() + "." + tuple.getFeatureId());
        mo.snippet(tuple.getStoreId());
        return gmap.addMarker(mo);
    }
}
