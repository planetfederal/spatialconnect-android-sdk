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
import com.boundlessgeo.spatialconnect.messaging.MessageType;
import com.boundlessgeo.spatialconnect.messaging.SCConfigMessage;
import com.boundlessgeo.spatialconnect.scutilities.Json.ObjectMappers;
import com.boundlessgeo.spatialconnect.scutilities.Storage.SCFileUtilities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;

/**
 * The SCConfigService is responsible for managing the configuration for SpatialConnect.  This includes downloading
 * remote configuration and sweeping the external storage for config files, if required.  As configs are loaded, the
 * service will emit ConfigEvents to subscribers.
 */
public class SCConfigService extends SCService {

    private Context context;
    private static final String CONFIGS_DIR = "configs";
    private final String LOG_TAG = SCConfigService.class.getSimpleName();
    private ArrayList<File> localConfigFiles = new ArrayList<>();

    public SCConfigService(Context context, Observable messages) {
        this.context = context;
        this.messages = messages;
    }

    /**
     * Loads all config files and registers the stores for each config.
     * <p/>
     * <p>First, we try to load the configs from external storage.  Note that this supports the "no network" use case
     * where the SDcard has data on it and the config file points to that data.</p>
     * <p>Second, we try to download any configuration files from the SpatialConnect backend service.</p>
     * <p>Third, we try to load any additional configs that were added by the user.</p>
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
            if (configFiles.length > 0) {
                registerDataStores(Arrays.asList(configFiles));
            }
            else {
                Log.d(LOG_TAG, "No config files found in external storage directory.");
            }
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
        Log.i(LOG_TAG, "Attempting to load remote config files.");
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
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        loadConfigs(Arrays.asList(scConfigFile));
    }

    public void loadLocalConfigs() {
        loadConfigs(this.getLocalConfigFiles());
    }

    // method to load the configuration from a file.  currently it only handles data store configurations.
    private void loadConfigs(List<File> configFiles) {
        registerDataStores(configFiles);
    }

    /* Registers all the stores specified in each config file */
    private void registerDataStores(List<File>  configFiles) {
        for (File file : configFiles) {
            Log.d(LOG_TAG, "Registering stores for config " + file.getPath());
            final Stores stores;
            try {
                // parse the "stores" attribute from the scconfig file
                stores = ObjectMappers.getMapper().readValue(file, Stores.class);

                // publish an event for each new store config
                for (SCStoreConfig storeConfig : stores.getStores()) {
                    Log.d(LOG_TAG, "Sending ADD_STORE_CONFIG message to CONFIGSERVICE for " +
                            storeConfig.getUniqueID() + " " + storeConfig.getName());
                    SCServiceManager.BUS.onNext(
                            new SCConfigMessage(
                                    MessageType.ADD_STORE_CONFIG,
                                    "CONFIGSERVICE",
                                    storeConfig
                            )
                    );
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
        this.localConfigFiles.add(configFile);
    }

    public ArrayList<File> getLocalConfigFiles() {
        return localConfigFiles;
    }

    @Override
    public void start() {
        messages.ofType(SCConfigMessage.class).subscribe(new Subscriber<SCConfigMessage>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(SCConfigMessage scConfigMessage) {
                if (scConfigMessage.getType().equals(MessageType.ADD_STORE_CONFIG)
                        && scConfigMessage.getServiceId().equals("CONFIGSERVICE")) {
                    Log.d(LOG_TAG, "Sending ADD_STORE_CONFIG message to DATASERVICE for store " +
                            scConfigMessage.getStoreConfig().getName());
                    // send a message to the data service
                    SCServiceManager.BUS.onNext(
                            new SCConfigMessage(
                                    MessageType.ADD_STORE_CONFIG,
                                    "DATASERVICE",
                                    scConfigMessage.getStoreConfig()  // pass along the config
                            )
                    );
                }
            }
        });
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
