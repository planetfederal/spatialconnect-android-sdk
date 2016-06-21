package com.boundlessgeo.spatialconnect.stores;

import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DefaultStore extends GeoPackageStore {

    private final String LOG_TAG = DefaultStore.class.getSimpleName();
    public static final String NAME = "DEFAULT_STORE";
    private ArrayList<SCFormConfig> formConfigs = new ArrayList<>();

    /**
     * Constructor for GeoPackageStore that initializes the data store adapter
     * based on the scStoreConfig.
     *
     * @param context       instance of the current activity's context
     * @param scStoreConfig instance of the configuration needed to configure the store
     */
    public DefaultStore(Context context, SCStoreConfig scStoreConfig) {
        super(context, scStoreConfig);
    }


    public void addFormLayer(SCFormConfig formConfig) {
        Log.d(LOG_TAG, "Saving form config " + formConfig.getName());
        formConfigs.add(formConfig);
        ((GeoPackageAdapter) getAdapter()).addFormLayer(formConfig);
    }

    public void deleteFormLayer(String layerName) {
        Iterator<SCFormConfig> itr = formConfigs.iterator();
        while (itr.hasNext()) {
            if (itr.next().getName().equals(layerName)) {
                itr.remove();
            }
        }
        ((GeoPackageAdapter) getAdapter()).deleteFormLayer(layerName);
    }

    public List<SCFormConfig> getFormConfigs() {
        return formConfigs;
    }
}
