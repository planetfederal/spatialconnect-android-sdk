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
package com.boundlessgeo.spatialconnect.sync;

import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;

public class SyncItem {
    int operation;
    SCSpatialFeature feature;

    public SyncItem(SCSpatialFeature f, int op) {
        this.operation = op;
        this.feature = f;
    }

    public int getOperation() {
        return operation;
    }

    public void setOperation(int operation) {
        this.operation = operation;
    }

    public SCSpatialFeature getFeature() {
        return feature;
    }

    public void setFeature(SCSpatialFeature feature) {
        this.feature = feature;
    }
}
