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

package com.boundlessgeo.spatialconnect.stores;


import android.content.Context;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class SCDataStore implements SCSpatialStore
{

    private SCDataAdapter adapter;
    // TODO: should we use the SCStoreConfig object instead of the store id?
    private String storeId;
    private String name;
    private int version = 0;
    private String type;
    private Context context;
    private SCDataStoreStatus status = SCDataStoreStatus.DATA_STORE_STOPPED;
    private String defaultLayerName;

    public SCDataStore(Context context, SCStoreConfig scStoreConfig)
    {
        this.context = context;
        if (scStoreConfig.getUniqueID() != null) {
            this.storeId = scStoreConfig.getUniqueID();
        } else {
            this.storeId = UUID.randomUUID().toString();
        }
    }

    public SCDataAdapter getAdapter()
    {
        return this.adapter;
    }

    public void setAdapter(SCDataAdapter adapter)
    {
        this.adapter = adapter;
    }

    public String getStoreId()
    {
        return this.storeId;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getVersion()
    {
        return this.version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public String getType()
    {
        return this.type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getKey()
    {
        return this.type + "." + this.version;
    }

    public Context getContext()
    {
        return this.context;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    public SCDataStoreStatus getStatus()
    {
        return this.status;
    }

    public void setStatus(SCDataStoreStatus status)
    {
        this.status = status;
    }

    public String getDefaultLayerName() {
        return defaultLayerName;
    }

    public void setDefaultLayerName(String defaultLayerName) {
        this.defaultLayerName = defaultLayerName;
    }

    public Map<String, String> toMap()
    {
        Map<String, String> storeMap = new HashMap<>();
        storeMap.put("storeId", this.storeId);
        storeMap.put("name", this.name);
        storeMap.put("type", this.type);
        storeMap.put("version", Integer.toString(this.version));
        storeMap.put("key", getKey());
        return storeMap;
    }

    @Override
    public String toString() {
        return storeId + "." + name;
    }
}
