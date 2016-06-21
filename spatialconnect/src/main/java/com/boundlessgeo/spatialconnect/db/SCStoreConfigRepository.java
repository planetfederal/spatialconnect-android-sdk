/*
 * Copyright 2016 Boundless, http://boundlessgeo.com
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

package com.boundlessgeo.spatialconnect.db;

import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;

public class SCStoreConfigRepository {

    private SCKVPStore kvpStore;
    private final String LOG_TAG = SCStoreConfigRepository.class.getSimpleName();


    public SCStoreConfigRepository(Context context) {
        kvpStore = new SCKVPStore(context);
    }

    public void addStoreConfig(SCStoreConfig store) {
        Log.d(LOG_TAG, "Adding store config to kvp.db.  Store: " + store.getName());
        String storePrefix = "stores." + store.getUniqueID() + ".";
        this.kvpStore.put(storePrefix + "id", store.getUniqueID());
        this.kvpStore.put(storePrefix + "type", store.getType());
        this.kvpStore.put(storePrefix + "version", store.getVersion());
        this.kvpStore.put(storePrefix + "name", store.getName());
        this.kvpStore.put(storePrefix + "uri", store.getUri());
        this.kvpStore.close();
    }

    public int getNumberOfStores() {
        return kvpStore.getValuesForKeyPrefix("stores.%.id").size();
    }
}
