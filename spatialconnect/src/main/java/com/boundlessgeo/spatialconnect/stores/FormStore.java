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

package com.boundlessgeo.spatialconnect.stores;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCFormField;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.db.SCSqliteHelper;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.scutilities.Json.JsonUtilities;
import com.boundlessgeo.spatialconnect.style.SCStyle;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * A store that persists/queries form submissions to/from a GeoPackage
 */
public class FormStore extends GeoPackageStore implements ISCSpatialStore, SCDataStoreLifeCycle {

    private final String LOG_TAG = FormStore.class.getSimpleName();
    public static final String NAME = "FORM_STORE";
    private Map<String, SCFormConfig> storeForms = new HashMap<>();
    private Map<String, String> formIds = new HashMap<>();
    public BehaviorSubject<Boolean> hasForms = BehaviorSubject.create(false);

    /**
     * Constructor for FormStore that initializes the data store
     * based on the scStoreConfig.
     *
     * @param context       instance of the current activity's context
     * @param scStoreConfig instance of the configuration needed to configure the store
     */
    public FormStore(Context context, SCStoreConfig scStoreConfig) {
        this(context, scStoreConfig, null);
    }

    public FormStore(Context context, SCStoreConfig scStoreConfig, SCStyle style) {
        super(context, scStoreConfig, style);
    }

    public void registerFormByConfig(SCFormConfig formConfig) {
        addLayerByConfig(formConfig);
    }

    public void updateFormByConfig(SCFormConfig formConfig) {
        addLayerByConfig(formConfig);
    }

    public void unregisterFormByConfig(SCFormConfig formConfig) {
        storeForms.remove(formConfig.getFormKey());
        formIds.remove(formConfig.getFormKey());
        hasForms.onNext(storeForms.size() > 0);
    }

    public void unregisterFormByKey(String key) {
        SCFormConfig config = storeForms.get(key);
        unregisterFormByConfig(config);
    }

    public void deleteFormLayer(String layerName) {
        storeForms.remove(layerName);
        deleteLayer(layerName);
    }

    public List<SCFormConfig> getFormConfigs() {
        List<SCFormConfig> configList = new ArrayList<>();
        for (SCFormConfig c : storeForms.values()) {
            configList.add(c);
        }

        return configList;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public Observable<Void> delete(final SCKeyTuple keyTuple) {
        return Observable.empty();
    }

    public Observable<SCSpatialFeature> update(final SCSpatialFeature scSpatialFeature) {
        return Observable.empty();
    }

    @Override
    public String syncChannel() {
        return "/store/form";
    }

    @Override
    public Map<String, Object> generateSendPayload(SCSpatialFeature scSpatialFeature) {
        HashMap<String, Object> formSubmissionPayload = new HashMap<>();
        SCFormConfig config = storeForms.get(scSpatialFeature.getKey().getLayerId());
        Integer formId = config != null ? Integer.parseInt(config.getId()) : null;
        if ( formId != null ) {
            formSubmissionPayload.put("form_id", formId);
            formSubmissionPayload.put("feature", scSpatialFeature);
        }
        else {
            Log.w(LOG_TAG, "Did not send feature b/c form id was null");
        }

        return formSubmissionPayload;
    }

    private void addLayerByConfig(SCFormConfig config) {
        Log.v(LOG_TAG, String.format("adding form config for %s", config.getFormKey()));
        boolean fieldsValid = true;

        Map<String, String> typeDefs = new HashMap<>();
        for (HashMap<String, Object> field : config.getFields()) {
            String fieldKey = JsonUtilities.getString(field, SCFormField.FIELD_KEY);
            if (!TextUtils.isEmpty(fieldKey)) {
                if (SCSqliteHelper.SQLITE_KEYWORDS.contains(fieldKey.toUpperCase())) {
                    fieldKey = fieldKey + "_";
                }
                typeDefs.put(fieldKey, SCFormField.getColumnType(field));
            } else {
                Log.w(LOG_TAG, String.format("Form field %s was invalid", fieldKey));
                fieldsValid = false;
                break;
            }
        }

        if (fieldsValid) {
            storeForms.put(config.getFormKey(), config);
            formIds.put(config.getFormKey(), config.getId());
            final String tableName = config.getFormKey();
            super.addLayer(tableName, typeDefs);
            hasForms.onNext(storeForms.size() > 0);
        }
    }
}
