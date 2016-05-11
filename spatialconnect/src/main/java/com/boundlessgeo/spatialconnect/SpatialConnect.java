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

import com.boundlessgeo.spatialconnect.messaging.SCConfigMessage;
import com.boundlessgeo.spatialconnect.messaging.SCMessage;
import com.boundlessgeo.spatialconnect.services.SCConfigService;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.services.SCKVPStoreService;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCService;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import rx.subjects.PublishSubject;

/**
 * When instantiated, SpatialConnect adds the default services and registers all stores
 * defined in the config file.  SpatialConnect is also used to manage the services (starting,
 * stopping, creating, deleting, etc).
 */
public class SpatialConnect {
    private HashMap<String, SCService> services;
    private SCDataService dataService;
    private SCKVPStoreService kvpStoreService;
    private SCSensorService sensorService;
    private SCConfigService configService;
    private Context context;
    private final String LOG_TAG = SpatialConnect.class.getSimpleName();

    /**
     * The bus is like an internal event bus for {@link SCMessage}s, its job is to receive {@link SCMessage}s from all
     * the services and publish them to any subscribers.
     */
    public static final PublishSubject<SCMessage> BUS = PublishSubject.create();

    /**
     * The default SpatialConnect constructor will scan through the app's config directory in its
     * <a href="http://developer.android.com/guide/topics/data/data-storage.html#filesExternal">external storage</a>
     * and register all stores defined within those config files.
     *
     * @param context
     */
    public SpatialConnect(Context context) {
        this.services = new HashMap<>();
        this.dataService = new SCDataService(context, BUS.ofType(SCConfigMessage.class));
        this.kvpStoreService = new SCKVPStoreService(context);
        this.sensorService = new SCSensorService(context);
        this.configService = new SCConfigService(context, BUS.ofType(SCConfigMessage.class));
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
        if (this.configService.getLocalConfigFiles().size() > 0) {
            this.configService.loadLocalConfigs();
        }
    }

    public void loadDefaultConfigs() {
        this.configService.loadConfigs();
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

    public void addConfig(File f) {
        this.configService.addConfig(f);
    }

    public SCConfigService getConfigService() {
        return configService;
    }
}

