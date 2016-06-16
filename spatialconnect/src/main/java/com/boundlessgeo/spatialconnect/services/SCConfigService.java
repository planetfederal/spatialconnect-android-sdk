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
import android.os.Environment;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCConfig;
import com.boundlessgeo.spatialconnect.config.SCFormConfig;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.scutilities.Json.ObjectMappers;
import com.boundlessgeo.spatialconnect.scutilities.Storage.SCFileUtilities;
import com.boundlessgeo.spatialconnect.stores.DefaultStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The SCConfigService is responsible for managing the configuration for SpatialConnect.  This includes downloading
 * remote configuration and sweeping the external storage for config files, if required.
 */
public class SCConfigService extends SCService {

    private static final String LOG_TAG = SCConfigService.class.getSimpleName();
    private Context context;
    private static final String CONFIGS_DIR = "configs";
    private ArrayList<File> localConfigFiles = new ArrayList<>();
    private SCDataService dataService;
    private SCNetworkService networkService;

    public SCConfigService(Context context) {
        this.context = context;
        this.dataService = SpatialConnect.getInstance().getDataService();
        this.networkService = SpatialConnect.getInstance().getNetworkService();
    }

    /**
     * Loads all config files and registers the stores for each config.
     * <p/>
     * <p>First, we try to load the default config from internal storage, then we try to load other configs from the
     * external storage folder.  Note that both internal and external storage supports the "no network" use case
     * where the SDcard has data on it and the config file points to that data.  Next, we try to download any
     * configuration files from the SpatialConnect backend service defined in the {@code remote} attribute of a
     * config.  Then we load any configs that were added with {@link SCConfigService#addConfig(File)}.</p>
     */
    public void loadConfigs() {
        if (getDefaultConfig() != null) {
            loadConfigs(Arrays.asList(getDefaultConfig()));
        }
        loadConfigs(getConfigFilesFromExternalStorage());
        loadConfigs(this.getLocalConfigFiles());
    }

    /**
     * Get configs (valid JSON files with an ".scfg" extension) packaged within the app's
     * <a href="http://developer.android.com/guide/topics/data/data-storage.html#filesExternal">external storage</a>
     * directory for config files.
     */
    private List<File> getConfigFilesFromExternalStorage() {
        if (isExternalStorageWritable()) {
            File configsDir = context.getExternalFilesDir(CONFIGS_DIR);
            if (configsDir == null || !configsDir.exists()) {
                configsDir.mkdir();
            }
            Log.i(LOG_TAG, "Searching for config files in external storage directory " + configsDir.toString());
            File[] configFiles = SCFileUtilities.findFilesByExtension(configsDir, ".scfg");
            if (configFiles.length > 0) {
                return Arrays.asList(configFiles);
            }
            else {
                Log.d(LOG_TAG, "No config files found in external storage directory.");
            }
        }
        return new ArrayList<>();
    }

    /**
     * Loads the default configuration file from the app's internal files directory.  Note that the configs.scfg file
     * must be copied to the internal files directory by the application using this sdk.
     */
    private File getDefaultConfig() {
        Log.d(LOG_TAG, "Getting default config from "+ context.getFilesDir() + "/config.scfg");
        File[] configFiles = SCFileUtilities.findFilesByExtension(context.getFilesDir(), "config.scfg");
        if (configFiles.length > 0) {
            return configFiles[0];
        }
        else {
            Log.w(LOG_TAG, "No default config file found in internal storage directory.");
        }
        return null;
    }

    /**
     * Loads the config from the API specified in theUrl
     * @param theUrl api endpoint for config
     */
    private void loadRemoteConfig(String theUrl) {
        Log.i(LOG_TAG, "Attempting to load remote config.");
        loadConfigs(Arrays.asList(networkService.getFileBlocking(theUrl)));
    }

    // method to load the configuration from a file.
    private void loadConfigs(List<File> configFiles) {
        registerDataStores(configFiles);
        registerForms(configFiles);
        String remoteUrl = getRemoteConfigUrl(configFiles);
        if (remoteUrl != null) {
            String configPath = remoteUrl.endsWith("/") ? "config" : "/config";
            loadRemoteConfig(remoteUrl + configPath);
        }
    }

    /**
     * Returns the value of the first {@code remote} attribute defined in the config files.
     *
     * @param configFiles
     * @return the api endpoint for fetching the config
     */
    private String getRemoteConfigUrl(List<File> configFiles) {
        Log.d(LOG_TAG, "Locating api url for config");
        for (File file : configFiles) {
            final SCConfig scConfig;
            try {
                scConfig = ObjectMappers.getMapper().readValue(file, SCConfig.class);
                if(scConfig.getRemote() != null) {
                    return scConfig.getRemote();
                }
                else {
                    Log.w(LOG_TAG, "No remote attribute was present in the config file " + file.getPath());
                }
            }
            catch (IOException e) {
                // TODO: test for invalid configs
                e.printStackTrace();
            }
        }
        return null;
    }

    /* Registers all the forms specified in each config file */
    private void registerForms(List<File> configFiles) {
        for (File file : configFiles) {
            Log.d(LOG_TAG, "Registering forms for config file " + file.getPath());
            final SCConfig scConfig;
            try {
                scConfig = ObjectMappers.getMapper().readValue(file, SCConfig.class);
                if(scConfig.getFormConfigs() != null) {
                    for (SCFormConfig formConfig : scConfig.getFormConfigs()) {
                        Log.d(LOG_TAG, "Creating table for form " + formConfig.getName());
                        DefaultStore store = dataService.getDefaultStore();
                        if (store != null) {
                            store.addFormLayer(formConfig);
                        }
                    }
                }
                else {
                    Log.w(LOG_TAG, "No forms were present in the config file " + file.getPath());
                }
            }
            catch (IOException e) {
                // TODO: test for invalid configs
                e.printStackTrace();
            }
        }
    }

    /* Registers all the stores specified in each config file */
    private void registerDataStores(List<File> configFiles) {
        for (File file : configFiles) {
            Log.d(LOG_TAG, "Registering stores for config file " + file.getPath());
            final SCConfig scConfig;
            try {
                // parse the "stores" attribute from the scconfig file
                scConfig = ObjectMappers.getMapper().readValue(file, SCConfig.class);
                if (scConfig.getStoreConfigs() != null) {
                    for (SCStoreConfig storeConfig : scConfig.getStoreConfigs()) {
                        dataService.addNewStore(storeConfig);
                    }
                }
            }
            catch (IOException e) {
                // TODO: test for invalid configs
                e.printStackTrace();
            }
        }
    }

    /**
     * If application developers want to store their configs within the APK itself, they can add the config using
     * this method instead of using the external storage or the network.
     */
    public void addConfig(File configFile) {
        Log.d(LOG_TAG, "Adding config file " + configFile.getPath());
        localConfigFiles.add(configFile);
    }

    public ArrayList<File> getLocalConfigFiles() {
        return localConfigFiles;
    }

    @Override
    public void start() {
        Log.d(LOG_TAG, "Starting SCConfig Service.  Loading all configs");
        loadConfigs();
        this.setStatus(SCServiceStatus.SC_SERVICE_RUNNING);
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
