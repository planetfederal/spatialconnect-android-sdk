/**
 * Copyright 2015-2016 Boundless, http://boundlessgeo.com
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.query;


import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryCollection;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;

public class SCPredicate {
    private SCBoundingBox filterBbox;
    private String key;
    private Object value;
    private SCGeometryPredicateComparison geometryComp;
    private SCPredicateComparison predicateComp;
    private GeometryFactory geometryFactory = new GeometryFactory();

    public SCPredicate(SCBoundingBox bbox, SCGeometryPredicateComparison comp) {
        this.filterBbox = bbox;
        this.geometryComp = comp;
    }

    public boolean applyFilter(SCSpatialFeature scSpatialFeature) {
        boolean status = false;
        if (scSpatialFeature instanceof SCGeometry) {
            status = applyFilterOnGeometry(scSpatialFeature);
        }
        if (scSpatialFeature instanceof SCGeometryCollection) {
            for (SCSpatialFeature scf : ((SCGeometryCollection) scSpatialFeature).getFeatures()) {
                if (scf instanceof SCGeometry) {
                    status = applyFilterOnGeometry(scf);
                }
            }
        }
        return status;
    }

    private boolean applyFilterOnGeometry(SCSpatialFeature feature) {
        boolean status = false;
        if (this.geometryComp == SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN) {
            status = isWithinBoundingBox((SCGeometry) feature);
        }
        else if (this.geometryComp == SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_NOTWITHIN) {
            status = !isWithinBoundingBox((SCGeometry) feature);
        }
        else if (this.geometryComp == SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_CONTAINS) {
            status = isContainedInBoundingBox((SCGeometry) feature);
        }
        return status;
    }

    /**
     * The geometry is contained in a bounding box if all of its points lie within the filter's bbox.
     *
     * @param scGeometry
     * @return
     */
    public boolean isContainedInBoundingBox(SCGeometry scGeometry) {
        Double[] bboxCoords = this.filterBbox.getBbox();
        Envelope envelope = new Envelope(bboxCoords[0], bboxCoords[2], bboxCoords[1], bboxCoords[3]);
        return geometryFactory.toGeometry(envelope).contains(scGeometry.getGeometry());
    }

    /**
     * The geometry is within a bounding box if one of its points lies within the filter's bbox.
     *
     * @param scGeometry
     * @return
     */
    public boolean isWithinBoundingBox(SCGeometry scGeometry) {
        Double[] bboxCoords = this.filterBbox.getBbox();
        Envelope envelope = new Envelope(bboxCoords[0], bboxCoords[2], bboxCoords[1], bboxCoords[3]);
        return scGeometry.getGeometry().within(geometryFactory.toGeometry(envelope));
    }

    public SCBoundingBox getBoundingBox() {
        return this.filterBbox;
    }
}
