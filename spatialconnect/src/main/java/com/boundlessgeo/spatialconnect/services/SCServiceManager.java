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
package com.boundlessgeo.spatialconnect.services;

import android.content.Context;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * When instantiated, the SCServiceManager adds the default services and registers all stores
 * defined in the config file.  The SCServiceManager is also used to manage the services (starting,
 * stopping, creating, deleting, etc).
 */
public class SCServiceManager {
    private HashMap<String, SCService> services;
    private SCDataService dataService;
    private SCKVPStoreService kvpStoreService;
    private SCSensorService sensorService;
    private SCConfigService configService;
    private Context context;
    private final String LOG_TAG = SCServiceManager.class.getSimpleName();

    /**
     * The default SCServiceManager constructor will scan through the app's config directory in its
     * <a href="http://developer.android.com/guide/topics/data/data-storage.html#filesExternal">external storage</a>
     * and register all stores defined within those config files.
     *
     * @param context
     */
    public SCServiceManager(Context context) {
        this.services = new HashMap<>();
        this.dataService = SCDataService.getInstance();
        this.kvpStoreService = new SCKVPStoreService(context);
        this.sensorService = new SCSensorService(context);
        this.configService = new SCConfigService(context);
        this.context = context;
        addDefaultServices();
        configService.loadConfigs();
    }

    /**
     * Constructor for instantiating a service manager with a config file (or group of files).  It will register all
     * the stores defined in the config file(s) passed in.  Note that this constructor will only attempt to load the
     * configurations passed into the method and will not attempt to load the configurations from the default locations.
     *
     * @param context
     * @param configFiles
     */
    public SCServiceManager(Context context, File... configFiles) {
        this.services = new HashMap<>();
        this.dataService = SCDataService.getInstance();
        this.kvpStoreService = new SCKVPStoreService(context);
        this.sensorService = new SCSensorService(context);
        this.configService = new SCConfigService(context);
        this.context = context;
        addDefaultServices();
        configService.loadConfigs(configFiles);
    }

    /**
     * Adds all the default services to the services hash map maintained by the SCServiceManager.
     */
    public void addDefaultServices() {
        addService(this.dataService);
        addService(this.sensorService);
        addService(this.configService);
        addService(this.kvpStoreService);
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

    public Context getContext() {
        return context;
    }
}

