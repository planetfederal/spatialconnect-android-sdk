package com.boundlessgeo.spatialconnect.stores;

import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Copyright 2016 Boundless http://boundlessgeo.com
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
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

public class FormStore extends GeoPackageStore {

    private final String LOG_TAG = FormStore.class.getSimpleName();
    public static final String NAME = "FORM_STORE";
    private ArrayList<SCFormConfig> formConfigs = new ArrayList<>();

    /**
     * Constructor for FormStore that initializes the data store
     * based on the scStoreConfig.
     *
     * @param context       instance of the current activity's context
     * @param scStoreConfig instance of the configuration needed to configure the store
     */
    public FormStore(Context context, SCStoreConfig scStoreConfig) {
        super(context, scStoreConfig);
    }

    public void addFormLayer(SCFormConfig formConfig) {
        Log.d(LOG_TAG, "Saving form config " + formConfig.getFormKey());
        formConfigs.add(formConfig);
        ((GeoPackageAdapter) getAdapter()).addFormLayer(formConfig);
    }

    public void deleteFormLayer(String layerName) {
        Iterator<SCFormConfig> itr = formConfigs.iterator();
        while (itr.hasNext()) {
            if (itr.next().getFormKey().equals(layerName)) {
                itr.remove();
            }
        }
        ((GeoPackageAdapter) getAdapter()).deleteFormLayer(layerName);
    }

    public List<SCFormConfig> getFormConfigs() {
        return formConfigs;
    }

}
