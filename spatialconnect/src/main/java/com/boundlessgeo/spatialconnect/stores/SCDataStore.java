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
import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Subclasses of SCDataStore provide read/write access to a single data store using an {@link SCDataAdapter}.  Instances
 * of SCDataStore are initialized with an {@link SCStoreConfig} which provides the information needed to connect to the
 * underlying data source.
 * <p></p>
 * Some implementations may throw {@link SCDataStoreException} if a problem arises that the client cannot recover from.
 * For example, trying to write to a table that doesn't exist will throw a {@link SCDataStoreException}.
 */
public abstract class SCDataStore {

    private SCDataAdapter adapter;
    private String storeId;
    private String name;
    private String version;
    private String type;
    private Context context;
    private SCDataStoreStatus status = SCDataStoreStatus.SC_DATA_STORE_STOPPED;
    private float downloadProgress;


    public SCDataStore(Context context, SCStoreConfig scStoreConfig) {
        this.context = context;
        if (scStoreConfig.getUniqueID() != null) {
            this.storeId = scStoreConfig.getUniqueID();
        }
        else {
            this.storeId = UUID.randomUUID().toString();
        }
    }

    public DataStorePermissionEnum getAuthorization() {
        return DataStorePermissionEnum.READ;
    }

    public SCDataAdapter getAdapter() {
        return this.adapter;
    }

    public void setAdapter(SCDataAdapter adapter) {
        this.adapter = adapter;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return this.type + "." + this.version;
    }

    public Context getContext() {
        return this.context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public SCDataStoreStatus getStatus() {
        return this.status;
    }

    public void setStatus(SCDataStoreStatus status) {
        this.status = status;
    }

    public float getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(float downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    public List<String> layers() {
        return null;
    }

    public String versionKey() {
        return type + "." + version;
    }

    public Map<String, String> toMap() {
        Map<String, String> storeMap = new HashMap<>();
        storeMap.put("storeId", this.storeId);
        storeMap.put("name", this.name);
        storeMap.put("type", this.type);
        storeMap.put("version", this.version);
        storeMap.put("key", getKey());
        return storeMap;
    }

    @Override
    public String toString() {
        return storeId + "." + name;
    }

    public enum DataStorePermissionEnum {
        READ, READ_WRITE
    }
}
