package com.boundlessgeo.spatialconnect.scutilities;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SCCache {
    private String LOG_TAG = SCCache.class.getSimpleName();
    private static final String CACHE_FILE = "signal";
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

