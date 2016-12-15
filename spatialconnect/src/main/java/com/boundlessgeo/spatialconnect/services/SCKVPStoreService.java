package com.boundlessgeo.spatialconnect.services;

import android.content.Context;

import com.boundlessgeo.spatialconnect.db.SCKVPStore;

import java.util.Map;

/**
 *  Provides an interface used to read/write to/from the SCKVPStore.
 *  TODO: react to config changes and update kvp rows accordingly
 */
//TODO:remove
public class SCKVPStoreService extends SCService {

    private SCKVPStore store;

    public SCKVPStoreService(Context context) {
        this.store = new SCKVPStore(context);
    }

    public void put(String key, Object value) {
        this.store.put(key, value);
    }

    public Map<String, Object> getValuesForKeyPrefix(String keyPrefix) {
        return this.store.getValuesForKeyPrefix(keyPrefix);
    }

    public Map<String, Object> getValueForKey(String key) {
        return this.store.getValueForKey(key);
    }

}
