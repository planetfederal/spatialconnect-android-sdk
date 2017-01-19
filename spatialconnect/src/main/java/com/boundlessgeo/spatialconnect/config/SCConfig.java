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
package com.boundlessgeo.spatialconnect.config;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SCConfig {

    @JsonProperty("stores")
    private List<SCStoreConfig> stores;

    @JsonProperty("forms")
    private List<SCFormConfig> forms;

    @JsonProperty("remote")
    private SCRemoteConfig remote;

    public SCConfig() {
    }

    public List<SCStoreConfig> getStores() {
        return stores;
    }

    public void setStoreConfigs(List<SCStoreConfig> configs) {
        this.stores = configs;
    }

    public List<SCFormConfig> getForms() {
        return forms;
    }

    public void setFormConfigs(List<SCFormConfig> forms) {
        this.forms = forms;
    }

    public SCRemoteConfig getRemote() {
        return remote;
    }

    public void setRemote(SCRemoteConfig remote) {
        this.remote = remote;
    }

    public void addStore(SCStoreConfig storeConfig) {
        stores.add(storeConfig);
    }

    public void updateStore(SCStoreConfig storeConfig) {
        for (int i = 0; i < stores.size(); i++) {
            if (stores.get(i).getUniqueID().equalsIgnoreCase(storeConfig.getUniqueID())) {
                stores.add(i, storeConfig);
                break;
            }
        }
    }

    public void removeStore(String id) {
        for (int i = 0; i < stores.size(); i++) {
            if (stores.get(i).getUniqueID().equalsIgnoreCase(id)) {
                stores.remove(i);
                break;
            }
        }
    }

    public void addForm(SCFormConfig formConfig) {
        forms.add(formConfig);
    }

    public void updateForm(SCFormConfig formConfig) {
        for (int i = 0; i < forms.size(); i++) {
            if (forms.get(i).getFormKey().equalsIgnoreCase(formConfig.getFormKey())) {
                forms.add(i, formConfig);
                break;
            }
        }
    }

    public void removeForm(String id) {
        for (int i = 0; i < forms.size(); i++) {
            if (forms.get(i).getFormKey().equalsIgnoreCase(id)) {
                forms.remove(i);
                break;
            }
        }
    }
}
