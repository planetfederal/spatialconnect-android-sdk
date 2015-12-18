package com.boundlessgeo.spatialconnect.scutilities;

import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
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
                    addPointToMap(gmap, point, geomFeature.getId(), geomFeature.getKey().getStoreId());
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

    private static void addPointToMap(GoogleMap gmap, SCGeometry feature) {
        addPointToMap(gmap, (Point) feature.getGeometry(), feature.getId(), feature.getKey().getStoreId());
    }

    private static void addPointToMap(GoogleMap gmap, Point point, String featureId, String storeId) {
        LatLng latLng = new LatLng(point.getY(), point.getX());
        MarkerOptions mo = new MarkerOptions();
        mo.position(latLng);
        mo.title(featureId);
        mo.snippet(storeId);
        gmap.addMarker(mo);
    }
}
