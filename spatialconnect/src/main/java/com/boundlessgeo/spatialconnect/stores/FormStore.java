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
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCFormField;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.services.SCBackendService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class FormStore extends GeoPackageStore implements  SCSpatialStore, SCDataStoreLifeCycle {

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
        super(context, scStoreConfig);
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
        ((GeoPackageAdapter) getAdapter()).deleteLayer(layerName);
    }

    public List<SCFormConfig> getFormConfigs() {
        List<SCFormConfig> configList = new ArrayList<>();
        for (SCFormConfig c : storeForms.values()) {
            configList.add(c);
        }

        return configList;
    }

    @Override
    public Observable<SCSpatialFeature> create(final SCSpatialFeature scSpatialFeature) {

        Observable<SCSpatialFeature> spatialFeature = ((GeoPackageAdapter) getAdapter()).create(scSpatialFeature);

        return spatialFeature.doOnCompleted(new Action0() {
            @Override
            public void call() {
                String formId;
                SCFormConfig c = storeForms.get(scSpatialFeature.getKey().getLayerId());
                formId = c.getId();
                if (formId != null) {
                    final String theUrl = SCBackendService.API_URL + "forms/" + formId + "/submit";
                    if (SCBackendService.API_URL != null) {
                        SCBackendService.networkConnected.subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean connected) {
                                if (connected) {
                                    Log.d(LOG_TAG, "Posting created feature to " + theUrl);
                                    HttpHandler.getInstance().post(theUrl, scSpatialFeature.toJson()).subscribe();
                                }
                            }
                        });
                    }
                }
                else {
                    Log.w(LOG_TAG, "Could not post feature b/c form id was null");
                }
            }
        });
    }

    @Override
    public Observable<Void> delete(final SCKeyTuple keyTuple) {
        return Observable.empty();
    }

    public Observable<SCSpatialFeature> update(final SCSpatialFeature scSpatialFeature) {
        return Observable.empty();
    }

    @Override
    public Observable<SCSpatialFeature> queryById(final SCKeyTuple keyTuple) {
        return Observable.empty();
    }

    private void addLayerByConfig(SCFormConfig config) {
        boolean fieldsValid = true;
        Map<String, String> typeDefs = new HashMap<>();
        for (SCFormField field : config.getFields()) {
            if (field.getKey() != null && field.getKey().length() > 0) {
                typeDefs.put(field.getKey(), field.getColumnType());
            } else {
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
