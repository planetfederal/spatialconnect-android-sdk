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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class FormStore extends GeoPackageStore {

    private final String LOG_TAG = FormStore.class.getSimpleName();
    public static final String NAME = "FORM_STORE";
    private ArrayList<SCFormConfig> formConfigs = new ArrayList<>();
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

    public void addFormLayer(SCFormConfig formConfig) {
        Log.d(LOG_TAG, "Saving form config " + formConfig.getFormKey());
        formConfigs.add(formConfig);
        final String tableName = formConfig.getFormKey();
        Map<String, String> typeDefs = new HashMap<>();
        for (SCFormField field : formConfig.getFields()) {
            typeDefs.put(field.getKey().replace(" ", "_").toLowerCase(), field.getColumnType());
        }
        super.addLayer(tableName, typeDefs);

        hasForms.onNext(true);
    }

    public void deleteFormLayer(String layerName) {
        Iterator<SCFormConfig> itr = formConfigs.iterator();
        while (itr.hasNext()) {
            if (itr.next().getFormKey().equals(layerName)) {
                itr.remove();
            }
        }
        ((GeoPackageAdapter) getAdapter()).deleteLayer(layerName);
    }

    public List<SCFormConfig> getFormConfigs() {
        return formConfigs;
    }

    @Override
    public Observable<SCSpatialFeature> create(final SCSpatialFeature scSpatialFeature) {

        Observable<SCSpatialFeature> spatialFeature = ((GeoPackageAdapter) getAdapter()).create(scSpatialFeature);

        return spatialFeature.doOnCompleted(new Action0() {
            @Override
            public void call() {
                String formId = "";
                for(SCFormConfig config : formConfigs) {
                    if (config.getFormKey().equals(scSpatialFeature.getKey().getLayerId())) {
                        formId = config.getId();
                    }
                }
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
}
