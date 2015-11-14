/*
 *
 *  * ****************************************************************************
 *  *  Licensed to the Apache Software Foundation (ASF) under one
 *  *  or more contributor license agreements.  See the NOTICE file
 *  *  distributed with this work for additional information
 *  *  regarding copyright ownership.  The ASF licenses this file
 *  *  to you under the Apache License, Version 2.0 (the
 *  *  "License"); you may not use this file except in compliance
 *  *  with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing,
 *  *  software distributed under the License is distributed on an
 *  *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  *  KIND, either express or implied.  See the License for the
 *  *  specific language governing permissions and limitations
 *  *  under the License.
 *  * ****************************************************************************
 *
 */

package com.boundlessgeo.spatialconnect.query;


import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryCollection;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;

public class SCPredicate
{
    private SCBoundingBox filterBbox;
    private String key;
    private Object value;
    private SCGeometryPredicateComparison geometryComp;
    private SCPredicateComparison predicateComp;

    public SCPredicate(SCBoundingBox bbox, SCGeometryPredicateComparison comp)
    {
        this.filterBbox = bbox;
        this.geometryComp = comp;
    }

    public boolean applyFilter(SCSpatialFeature scSpatialFeature)
    {
        boolean status = false;
        boolean contained = false;
        if(scSpatialFeature instanceof SCGeometryCollection)
        {
            for(SCSpatialFeature scf : ((SCGeometryCollection) scSpatialFeature).getFeatures())
            {
                if(scf instanceof SCGeometry)
                {
                    contained = isInBoundingBox((SCGeometry)scf);
                    if(this.geometryComp == SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN && contained
                            || this.geometryComp == SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_NOTWITHIN && !contained)
                    {
                        status = true;
                        break;
                    }
                }
            }
        }
        return status;
    }

    public boolean isInBoundingBox(SCGeometry scGeometry)
    {
        Double[] bboxCoords = this.filterBbox.getBbox();
        Double[] geometryCoords = scGeometry.getBbox();
        if((geometryCoords[0] >= bboxCoords[0] && geometryCoords[2] <= bboxCoords[2])
                && (geometryCoords[1] >= bboxCoords[1] && geometryCoords[3] <= bboxCoords[3]))
        {
            return true;
        }
        return false;
    }

}
