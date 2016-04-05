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

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.config.Stores;
import com.boundlessgeo.spatialconnect.db.SCStoreConfigRepository;
import com.boundlessgeo.spatialconnect.scutilities.Json.ObjectMappers;
import com.boundlessgeo.spatialconnect.scutilities.Storage.SCFileUtilities;
import com.boundlessgeo.spatialconnect.stores.GeoJsonStore;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * The SCConfigService is responsible for managing the configuration for SpatialConnect.  This includes downloading
 * remote configuration, persisting them locally, and managing application preferences.
 */
public class SCConfigService extends SCService {

    private Context context;
    private static final String CONFIGS_DIR = "configs";
    private final String LOG_TAG = SCConfigService.class.getSimpleName();

    public SCConfigService(Context context) {
        this.context = context;
    }

    /**
     * Loads all config files and registers the stores for each config.
     * <p/>
     * <p>First, we try to load the configs from external storage.  Note that this supports the "no network" use case
     * where the SDcard has data on it and the config file points to that data.</p>
     * <p>Second, we try to download any configuration files from the SpatialConnect backend service.</p>
     */
    public void loadConfigs() {
        loadConfigsFromExternalStorage();
        loadRemoteConfigs();
    }

    /**
     * Load configs (valid JSON files with an ".scfg" extension) packaged within the app's
     * <a href="http://developer.android.com/guide/topics/data/data-storage.html#filesExternal">external storage</a>
     * directory for config files.
     */
    private void loadConfigsFromExternalStorage() {
        if (isExternalStorageWritable()) {
            File configsDir = context.getExternalFilesDir(CONFIGS_DIR);
            if (configsDir == null || !configsDir.exists()) {
                configsDir.mkdir();
            }
            Log.i(LOG_TAG, "Searching for config files in external storage directory " + configsDir.toString());
            File[] configFiles = SCFileUtilities.findFilesByExtension(configsDir, ".scfg");
            registerDataStores(configFiles);
        }
    }

    /**
     * Loads the config from the API.
     *
     * TODO: We need to dynamically set the URL of the backend SpatialConnect service that this application will talk
     * to.  Currently we're just loading a config file stored in S3, but eventually we should use the SCNetworkService
     * to get the configs from the the SpatialConnect backend service.
     */
    private void loadRemoteConfigs() {
        OkHttpClient client = SCNetworkService.getHttpClient();
        Request request = new Request.Builder()
                .url("https://s3.amazonaws.com/test.spacon/scconfig.scfg")
                .build();

        Response response;
        File scConfigFile = null;
        try {
            response = client.newCall(request).execute();
            BufferedInputStream is = new BufferedInputStream(response.body().byteStream());
            scConfigFile = File.createTempFile(UUID.randomUUID().toString(), null, context.getCacheDir());
            OutputStream os = new FileOutputStream(scConfigFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            os.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadConfigs(scConfigFile);
    }


    /**
     * Secondary way to load configs if application developers want to store their configs within the APK itself,
     * instead of using the external storage or the network.  Note that this method will not attempt to load the configs
     * from the default locations.  If the developer wants to load the default configurations, they will need to
     * explicitly call the {@link SCConfigService#loadConfigs()} in addition to this method.
     *
     * @param configFiles
     */
    public void loadConfigs(File... configFiles) {
        registerDataStores(configFiles);
    }

    /* Registers all the stores specified in each config file */
    private void registerDataStores(File... configFiles) {
        for (File file : configFiles) {
            Log.d(LOG_TAG, "Registering stores for config " + file.getName());
            registerStores(file);
        }
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    /**
     * Reads a config file to determine what stores are defined, then registers each supported data store with the
     * dataService.
     *
     * @param f - a valid SpatialConnect config file
     */
    public void registerStores(File f) {
        try {
            final Stores stores = ObjectMappers.getMapper().readValue(f, Stores.class);
            final List<SCStoreConfig> scStoreConfigs = stores.getStores();
            for (SCStoreConfig scStoreConfig : scStoreConfigs) {
                String key = scStoreConfig.getType() + "." + scStoreConfig.getVersion();
                SCDataService dataService = SCDataService.getInstance();
                if (dataService.isStoreSupported(key)) {
                    if (key.equals("geojson.1")) {
                        dataService.registerStore(new GeoJsonStore(context, scStoreConfig));
                        Log.d(LOG_TAG, "Registered geojson.1 store " + scStoreConfig.getName());
                    }
                    else if (key.equals("gpkg.1")) {
                        dataService.registerStore(new GeoPackageStore(context, scStoreConfig));
                        Log.d(LOG_TAG, "Registered gpkg.1 store " + scStoreConfig.getName());
                    }
                }
                SCStoreConfigRepository storeDao = new SCStoreConfigRepository(context);
                storeDao.addStore(scStoreConfig);
            }
        } catch (Exception ex) {
            //TODO: test this with a bad config file.
            Log.w(LOG_TAG, "Couldn't register stores for " + f.getName(), ex);
        }
    }
}
