/**
 * Copyright 2015-2016 Boundless, http://boundlessgeo.com
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
package com.boundlessgeo.spatialconnect;

import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCRemoteConfig;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.services.SCAuthService;
import com.boundlessgeo.spatialconnect.services.SCBackendService;
import com.boundlessgeo.spatialconnect.services.SCConfigService;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.services.SCKVPStoreService;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCService;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import rx.functions.Action1;

/**
 * When instantiated, SpatialConnect adds the default services and registers all stores
 * defined in the config file.  SpatialConnect is also used to manage the services (starting,
 * stopping, creating, deleting, etc).
 */
public class SpatialConnect {

    private final String LOG_TAG = SpatialConnect.class.getSimpleName();
    private HashMap<String, SCService> services;
    private SCDataService dataService;
    private SCKVPStoreService kvpStoreService;
    private SCSensorService sensorService;
    private SCConfigService configService;
    private SCBackendService backendService;
    private SCAuthService authService;

    private Context context;

    private SpatialConnect() {}

    private static class SingletonHelper {
        private static final SpatialConnect INSTANCE = new SpatialConnect();
    }

    public static SpatialConnect getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Calling {@code SpatialConnect#initialize()} will scan through the app's config directory in its
     * <a href="http://developer.android.com/guide/topics/data/data-storage.html#filesExternal">external storage</a>
     * and register all stores defined within those config files.
     *
     * @param context
     */
    public void initialize(Context context) {
        Log.d(LOG_TAG, "Initializing SpatialConnect");
        this.services = new HashMap<>();
        this.sensorService = new SCSensorService(context);
        this.dataService = new SCDataService(context);
        this.kvpStoreService = new SCKVPStoreService(context);
        this.configService = new SCConfigService(context);
        this.authService = new SCAuthService(context);
        this.context = context;
        addDefaultServices();
    }

    /**
     * Adds all the default services to the services hash map maintained by the SpatialConnect.
     */
    private void addDefaultServices() {
        addService(this.dataService);
        addService(this.sensorService);
        addService(this.configService);
        addService(this.kvpStoreService);
        addService(this.authService);
    }

    public void addService(SCService service) {
        this.services.put(service.getId(), service);
    }

    public void removeService(SCService service) {
        this.services.remove(service.getId());
    }

    public void startService(String id) {
        this.services.get(id).start();
    }

    public void stopService(String id) {
        this.services.get(id).stop();
    }

    public void restartService(String id) {
        this.services.get(id).stop();
        this.services.get(id).start();
    }

    public void startAllServices() {
        Log.d(LOG_TAG, "Starting all services.");
        for (String key : this.services.keySet()) {
            this.services.get(key).start();
        }
    }

    public void stopAllServices() {
        for (String key : this.services.keySet()) {
            this.services.get(key).stop();
        }
    }

    public void restartAllServices() {
        for (String key : this.services.keySet()) {
            this.services.get(key).stop();
            this.services.get(key).start();
        }
    }

    public void connectBackend(final SCRemoteConfig remoteConfig) {
        if (remoteConfig != null && backendService == null) {
            Log.d(LOG_TAG, "connecting backend");
            backendService = new SCBackendService(context);

            /*
              if backendSvc available
                if no internet
                   load local cached remote config if present
                else
                       attempt to auth
                    if auth fails
                    load cache remote config if present
                    else
                    update remote config cache
             */
//            SCBackendService.networkConnected.subscribe(new Action1<Boolean>() {
//                @Override
//                public void call(Boolean connected) {
//                    if (connected) {
//                        Log.d(LOG_TAG, "connecting get remote from server");
//
//                    } else {
//                        //load config from cache
//                        Log.d(LOG_TAG, "No internet get cached remote config");
//                        //SpatialConnect.getInstance().getConfigService().loadConfigFromCache();
//
//                    }
//
//
//                }
//            });
            backendService.initialize(remoteConfig);
            backendService.start();
        }
    }

    public Context getContext() {
        return context;
    }

    public void addConfig(File f) {
        this.configService.addConfig(f);
    }

    public Map<String, SCService> getServices() {
        return services;
    }

    public SCService getServiceById(String id) {
        return this.services.get(id);
    }

    public SCDataService getDataService() {
        return dataService;
    }

    public SCSensorService getSensorService() {
        return sensorService;
    }

    public SCKVPStoreService getSCKVPStoreService() {
        return kvpStoreService;
    }

    public SCConfigService getConfigService() {
        return configService;
    }

    public SCBackendService getBackendService() {
        return backendService;
    }

    public SCAuthService getAuthService() {
        return authService;
    }
}

