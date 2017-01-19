/**
 * Copyright 2015-2017 Boundless, http://boundlessgeo.com
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
package com.boundlessgeo.spatialconnect.scutilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SCCache {
    private String LOG_TAG = SCCache.class.getSimpleName();
    private static final String CACHE_FILE = "spatialconnect";
    private SharedPreferences.Editor mEditor;
    private SharedPreferences mPrefs;

    public SCCache(final Context context) {
        mPrefs = context.getSharedPreferences(CACHE_FILE,
                Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();
    }

    public void setValue(Object value, String key) {
        try {
            if (value instanceof String) {
                mEditor.putString(key, (String) value).apply();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public String getStringValue(String key) {
        return mPrefs.getString(key, null);
    }
}

