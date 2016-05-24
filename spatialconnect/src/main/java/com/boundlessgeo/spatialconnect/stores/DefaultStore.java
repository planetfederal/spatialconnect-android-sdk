package com.boundlessgeo.spatialconnect.stores;

import android.content.Context;

import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;

public class DefaultStore extends GeoPackageStore {

    public static final String NAME = "DEFAULT_STORE";

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
        ((GeoPackageAdapter) getAdapter()).addFormLayer(formConfig);
    }

    public void deleteFormLayer(String layerName) {
        ((GeoPackageAdapter) getAdapter()).deleteFormLayer(layerName);
    }
}
