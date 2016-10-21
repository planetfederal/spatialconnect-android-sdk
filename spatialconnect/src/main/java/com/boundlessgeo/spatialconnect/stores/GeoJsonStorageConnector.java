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


import android.content.Context;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.scutilities.Storage.AndroidStorageType;
import com.boundlessgeo.spatialconnect.scutilities.Storage.SCFileUtilities;

import java.io.File;


public class GeoJsonStorageConnector
{
    private Context context;
    private SCStoreConfig scStoreConfig;
    private AndroidStorageType storageType;

    public GeoJsonStorageConnector() {}

    public GeoJsonStorageConnector(Context context, SCStoreConfig scStoreConfig)
    {
        this.context = context;
        this.scStoreConfig = scStoreConfig;
//        if(scStoreConfig.isMainBundle())
//        {
//            this.storageType = AndroidStorageType.INTERNAL;
//        }
//        else
//        {
//            this.storageType = AndroidStorageType.EXTERNAL;
//        }
    }

    public String getGeoJsonTextFromFile()
    {
        return SCFileUtilities.readTextFile(scStoreConfig.getUri(), storageType, context);
    }

    public File getFile()
    {
        if(this.storageType == AndroidStorageType.INTERNAL)
        {
            return SCFileUtilities.getInternalFileObject(scStoreConfig.getUri(), context);
        }
        else
        {
            return SCFileUtilities.getExternalFileObject(scStoreConfig.getUri());
        }
    }
}
