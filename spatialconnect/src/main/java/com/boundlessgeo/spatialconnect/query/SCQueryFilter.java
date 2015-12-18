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


import java.util.ArrayList;
import java.util.List;

public class SCQueryFilter
{
    //private List<SCPredicate> predicates;
    private SCPredicate predicate;
    private List<String> layerIds;
    private List<String> featureIds;

    public SCQueryFilter() {
        layerIds = new ArrayList<>();
        featureIds = new ArrayList<>();
    }

    public SCQueryFilter(SCPredicate scPredicate)
    {
        this.predicate = scPredicate;
    }

    public SCPredicate getPredicate()
    {
        return predicate;
    }

    public void addLayerId(String id) {
        layerIds.add(id);
    }

    public void addFeatureId(String id) {
        featureIds.add(id);
    }

}
