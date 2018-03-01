/**
 * Copyright 2018 Boundless, http://boundlessgeo.com
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

import android.util.Log;

import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.vividsolutions.jts.geom.Geometry;

import java.util.Map;

public class WFSUtils {
    private static String LOG_TAG = WFSUtils.class.getSimpleName();

    private WFSUtils(){}

    public static String buildWFSTInsertPayload(SCSpatialFeature scSpatialFeature, String remoteUrl) {
        // assumes "geonode" will always be the GeoServer workspace for Exchange backend
        String featureTypeUrl = String.format("%s/geoserver/wfs/DescribeFeatureType?typename=%s:%s",
                remoteUrl, "geonode", scSpatialFeature.getLayerId());

        return String.format(WFST_INSERT_TEMPLATE,
                featureTypeUrl,
                scSpatialFeature.getLayerId(),
                buildPropertiesXml(scSpatialFeature),
                buildGeometryXml(scSpatialFeature));
    }

    public static String buildWFSTUpdatePayload(SCSpatialFeature scSpatialFeature, String remoteUrl) {
        return String.format(WFST_UPDATE_TEMPLATE,
                scSpatialFeature.getLayerId(),
                buildPropertiesXml(scSpatialFeature),
                buildFilterXml(scSpatialFeature));
    }

    public static String buildWFSTDeletePayload(SCSpatialFeature scSpatialFeature, String remoteUrl) {
        return String.format(WFST_DELETE_TEMPLATE,
                scSpatialFeature.getLayerId(),
                buildFilterXml(scSpatialFeature));
    }

    private static String buildFilterXml(SCSpatialFeature feature) {
        String ogcFilter = "<ogc:Filter>\n"
                            + "   <ogc:FeatureId fid=\"%1$s\"/>\n"
                            +"</ogc:Filter>";
        return String.format(ogcFilter, feature.getId());
    }
    private static String buildPropertiesXml(SCSpatialFeature scSpatialFeature) {
        Map<String, Object> properties = scSpatialFeature.getProperties();
        StringBuilder sb = new StringBuilder();
        for (String key : properties.keySet()) {
            if (properties.get(key) != null) {
                sb.append(String.format("<%1$s>%2$s</%1$s>\n", key, properties.get(key)));
            }
        }
        return sb.toString();
    }

    private static String buildGeometryXml(SCSpatialFeature scSpatialFeature) {
        if (scSpatialFeature instanceof SCGeometry) {
            if (((SCGeometry) scSpatialFeature).getGeometry() != null) {
                Geometry geom = ((SCGeometry) scSpatialFeature).getGeometry();
                String type = geom.getGeometryType();
                // todo: find geometry column name
                String geometryColumnName = "wkb_geometry";
                if (type.equalsIgnoreCase("point")) {
                    //Because we are using WFS 1.0.0 we specify the order in lon/lat
                    //see http://docs.geoserver.org/stable/en/user/services/wfs/basics.html#wfs-basics-axis
                    return String.format(POINT_XML_TEMPLATE,
                            geometryColumnName,
                            geom.getCoordinate().y,
                            geom.getCoordinate().x);
                }
            }
        }
        Log.w(LOG_TAG, String.format("Feature %s did not have a geometry", scSpatialFeature.getId()));
        return "";
    }


    private Double validateDouble(Object o) {
        Double val = null;
        if (o instanceof Number) {
            val = ((Number) o).doubleValue();
        }
        return val;
    }

    private static final String WFST_INSERT_TEMPLATE =
            "<wfs:Transaction service=\"WFS\" version=\"1.0.0\"\n"
                    + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\n"
                    + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "  xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-transaction.xsd %1$s\">\n"
                    + "  <wfs:Insert>\n"
                    + "    <%2$s>\n"
                    + "      %3$s"
                    + "      %4$s"
                    + "    </%2$s>\n"
                    + "  </wfs:Insert>\n"
                    + "</wfs:Transaction>";

    private static final String WFST_UPDATE_TEMPLATE =
            "<wfs:Transaction service=\"WFS\" version=\"1.0.0\"\n"
                    + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\n"
                    + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "  xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-transaction.xsd \">\n"
                    + "  <wfs:Update typeName=\"%1$s\">\n"
                    + "      %2$s"
                    + "      %3$s"
                    + "  </wfs:Update>\n"
                    + "</wfs:Transaction>";

    private static final String WFST_DELETE_TEMPLATE =
            "<wfs:Transaction service=\"WFS\" version=\"1.0.0\"\n"
                    + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\n"
                    + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "  xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-transaction.xsd \">\n"
                    + "  <wfs:Delete typeName=\"%1$s\">\n"
                    + "      %2$s"
                    + "  </wfs:Delete>\n"
                    + "</wfs:Transaction>";

    private static final String POINT_XML_TEMPLATE =
            "<%1$s>\n"
                    + "        <gml:Point srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
                    + "          <gml:coordinates decimal=\".\" cs=\",\" ts=\" \">%2$f,%3$f</gml:coordinates>\n"
                    + "        </gml:Point>\n"
                    + "      </%1$s>\n";
}
