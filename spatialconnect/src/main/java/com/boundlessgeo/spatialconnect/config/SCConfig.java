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

    @JsonProperty("layers")
    private List<SCLayerConfig> layers;

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


    public List<SCLayerConfig> getLayers() {
        return layers;
    }

    public void setLayersConfigs(List<SCLayerConfig> layers) {
        this.layers = layers;
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

    public void addLayer(SCLayerConfig layerConfig) {
        layers.add(layerConfig);
    }

    public void updateLayer(SCLayerConfig layerConfig) {
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).getLayerKey().equalsIgnoreCase(layerConfig.getLayerKey())) {
                layers.add(i, layerConfig);
                break;
            }
        }
    }

    public void removeLayer(String id) {
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).getLayerKey().equalsIgnoreCase(id)) {
                layers.remove(i);
                break;
            }
        }
    }
}
