package com.boundlessgeo.spatialconnect.stores;

import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCFormField;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultStore extends GeoPackageStore implements  SCSpatialStore, SCDataStoreLifeCycle {

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
        Log.d(LOG_TAG, "Saving form config " + formConfig.getFormKey());
        formConfigs.add(formConfig);
        final String tableName = formConfig.getFormKey();
        Map<String, String> typeDefs = new HashMap<>();
        for (SCFormField field : formConfig.getFields()) {
            typeDefs.put(field.getKey().replace(" ", "_").toLowerCase(), field.getColumnType());
        }
        super.addLayer(tableName, typeDefs);
    }

    public void deleteFormLayer(String layerName) {
        Iterator<SCFormConfig> itr = formConfigs.iterator();
        while (itr.hasNext()) {
            if (itr.next().getFormKey().equals(layerName)) {
                itr.remove();
            }
        }
        super.deleteLayer(layerName);
    }

    public List<SCFormConfig> getFormConfigs() {
        return formConfigs;
    }
}
