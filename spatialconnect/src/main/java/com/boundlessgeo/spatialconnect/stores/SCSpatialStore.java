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
package com.boundlessgeo.spatialconnect.stores;


import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;

import java.util.List;

import rx.Observable;

/**
 * Interface definition for the spatial store.
 */
public interface SCSpatialStore
{
    Observable query(SCQueryFilter scFilter);
    Observable queryById(SCKeyTuple keyTuple);
    Observable create(SCSpatialFeature scSpatialFeature);
    Observable update(SCSpatialFeature scSpatialFeature);
    Observable delete(SCKeyTuple keyTuple);
    List<String> vectorLayers();
}
