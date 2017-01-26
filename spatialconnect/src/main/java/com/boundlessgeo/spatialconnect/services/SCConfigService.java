/*
 * Copyright 2016 Boundless, http://boundlessgeo.com
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

package com.boundlessgeo.spatialconnect.services;

import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCConfig;
import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.scutilities.Json.SCObjectMapper;
import com.boundlessgeo.spatialconnect.scutilities.Storage.SCFileUtilities;
import com.boundlessgeo.spatialconnect.stores.FormStore;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;

/**
 * The SCConfigService is responsible for managing the configuration for SpatialConnect.  This includes downloading
 * remote configuration and sweeping the external storage for config files, if required. The config service is
 * also responsible for parsing the relevant parts of the config and invoking functions on other services using parts
 * of the {@link SCConfig}.
 */
public class SCConfigService extends SCService implements SCServiceLifecycle {

    private static final String LOG_TAG = SCConfigService.class.getSimpleName();
    private static final String SERVICE_NAME = "SC_CONFIG_SERVICE";
    private static final String CONFIGS_DIR = "configs";
    private Context context;
    private List<String> configPaths = new ArrayList<>();
    private SpatialConnect sc;
    public SCConfigService(Context context) {
        this.context = context;
        sc = SpatialConnect.getInstance();
        sweepDataDirectory();
    }

    public void addConfigFilePath(String fp) {
        configPaths.add(fp);
    }

    public void addConfigFilepaths(List<String> fps) {
        configPaths.addAll(fps);
    }

    public void sweepDataDirectory() {
        File[] configFiles = SCFileUtilities.findFilesByExtension(context.getFilesDir(), ".scfg");
        if (configFiles.length > 0) {
            for (File file : configFiles) {
                configPaths.add(file.getAbsolutePath());
            }
        }
    }

    public void loadConfigs() {
        for (String path: configPaths) {
            File config = new File(path);
            final SCConfig scConfig;
            try {
                scConfig = SCObjectMapper.getMapper().readValue(config, SCConfig.class);
                loadConfig(scConfig);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadConfig(SCConfig config) {
        loadForms(config.getForms());
        loadDataStores(config.getStores());
        if (config.getRemote() != null) {
            sc.connectBackend(config.getRemote());
        }
    }

    public void addForm(SCFormConfig c) {
        sc.getDataService().getFormStore().registerFormByConfig(c);
    }

    public void removeForm(SCFormConfig c) {
        sc.getDataService().getFormStore().unregisterFormByConfig(c);
    }

    public void addStore(SCStoreConfig c) {
        sc.getDataService().registerAndStartStoreByConfig(c);
    }

    public void removeStore(SCStoreConfig c) {
        SCDataService dataService = sc.getDataService();
        dataService.unregisterStore(dataService.getStoreByIdentifier(c.getUniqueID()));
    }

    public void setCachedConfig(SCConfig config) {

        try {
            String configJson = SCObjectMapper.getMapper().writeValueAsString(config);
            sc.getCache().setValue(configJson, "spatialconnect.config.remote.cached");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    public SCConfig getCachedConfig() {
        SCConfig returnConfig = null;
        try {
            String configJson = sc.getCache().getStringValue("spatialconnect.config.remote.cached");
            if (configJson != null) {
                returnConfig = SCObjectMapper.getMapper().readValue(
                        configJson,
                        SCConfig.class);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnConfig;
    }

    @Override
    public Observable<Void> start() {
        super.start();
        loadConfigs();
        return Observable.empty();
    }

    @Override
    String getId() {
        return SERVICE_NAME;
    }

    /* Registers all the forms specified in each config file */
    private void loadForms(List<SCFormConfig> formConfigs) {
        if (formConfigs != null) {
            Log.d(LOG_TAG, "Loading "+ formConfigs.size() +" form configs");
            for (SCFormConfig formConfig : formConfigs) {
                Log.d(LOG_TAG, "Creating table for form " + formConfig.getFormKey());
                FormStore store = sc.getDataService().getFormStore();
                if (store != null) {
                    store.registerFormByConfig(formConfig);
                }
            }
        }
    }

    /* Registers all the stores specified in each config file */
    private void loadDataStores(List<SCStoreConfig> storeConfigs) {
        if (storeConfigs != null) {
            Log.d(LOG_TAG, "Loading "+ storeConfigs.size() +" store configs");
            for (SCStoreConfig storeConfig : storeConfigs) {
                Log.d(LOG_TAG, "Adding store " + storeConfig.getName() + " to data service.");
                try {
                    sc.getDataService().registerAndStartStoreByConfig(storeConfig);
                } catch (Exception ex) {
                    Log.w(LOG_TAG, "Exception adding store to data service ", ex);
                }
            }
        }
    }

    public static String serviceId() {
        return SERVICE_NAME;
    }
}