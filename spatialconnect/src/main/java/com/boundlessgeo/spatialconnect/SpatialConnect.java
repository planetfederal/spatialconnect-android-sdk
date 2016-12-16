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
import com.boundlessgeo.spatialconnect.scutilities.SCCache;
import com.boundlessgeo.spatialconnect.services.SCAuthService;
import com.boundlessgeo.spatialconnect.services.SCBackendService;
import com.boundlessgeo.spatialconnect.services.SCConfigService;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCService;
import com.boundlessgeo.spatialconnect.services.SCServiceStatus;
import com.boundlessgeo.spatialconnect.services.SCServiceStatusEvent;
import com.github.rtoshiro.secure.SecureSharedPreferences;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;

/**
 * When instantiated, SpatialConnect adds the default services and registers all stores
 * defined in the config file.  SpatialConnect is also used to manage the services (starting,
 * stopping, creating, deleting, etc).
 */
public class SpatialConnect {

    private final String LOG_TAG = SpatialConnect.class.getSimpleName();
    private HashMap<String, SCService> services;
    private SCDataService dataService;
    private SCSensorService sensorService;
    private SCConfigService configService;
    private SCBackendService backendService;
    private SCAuthService authService;
    private SCCache cache;

    private Context context;
    public BehaviorSubject<SCServiceStatusEvent> serviceEventSubject;
    public ConnectableObservable<SCServiceStatusEvent> serviceEvents;

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

        this.serviceEventSubject = BehaviorSubject.create();
        this.serviceEvents = serviceEventSubject.publish();

        this.services = new HashMap<>();
        this.sensorService = new SCSensorService(context);
        this.dataService = new SCDataService(context);
        this.configService = new SCConfigService(context);
        this.authService = new SCAuthService(context);
        this.cache = new SCCache(context);
        this.context = context;
        addDefaultServices();
    }

    /**
     * Adds all the default services to the services hash map maintained by the SpatialConnect.
     */
    private void addDefaultServices() {
        addService(SCDataService.serviceId(), this.dataService);
        addService(SCSensorService.serviceId(), this.sensorService);
        addService(SCConfigService.serviceId(), this.configService);
        addService(SCAuthService.serviceId(), this.authService);
    }

    public void addService(String serviceKey, SCService service) {
        this.services.put(serviceKey, service);
    }

    public void removeService(String id) {
        this.services.remove(id);
    }

    public void startService(final String id) {
        this.services.get(id).start().subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {}
            },
                new Action1<Throwable>() {
                @Override
                public void call(Throwable t) {
                    Log.d(LOG_TAG, t.getLocalizedMessage());
                    // onError can happen if we cannot start the store b/c of some error or runtime exception
                    serviceEventSubject.onNext(
                            new SCServiceStatusEvent(SCServiceStatus.SC_SERVICE_ERROR, id)
                    );
                }
            }, new Action0() {
                @Override
                public void call() {
                    serviceEventSubject.onNext(
                            new SCServiceStatusEvent(SCServiceStatus.SC_SERVICE_STARTED, id));
                }
        });
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

        HashMap<String, SCService> ss  = new HashMap<>(services);
        for (String key : ss.keySet()) {
            Log.e(LOG_TAG, "starting: " + key);
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

    public Observable<SCServiceStatusEvent> serviceStarted(final String serviceId) {
        return Observable.create(new Observable.OnSubscribe<SCServiceStatusEvent>() {
            @Override
            public void call(final Subscriber<? super SCServiceStatusEvent> subscriber) {
                SCService service = getServiceById(serviceId);
                if (service != null && service.getStatus() == SCServiceStatus.SC_SERVICE_RUNNING) {
                    subscriber.onNext(new SCServiceStatusEvent(SCServiceStatus.SC_SERVICE_STARTED));
                    subscriber.onCompleted();
                } else {
                    serviceEvents.autoConnect()
                            .subscribe(new Action1<SCServiceStatusEvent>() {
                                @Override
                                public void call(SCServiceStatusEvent event) {
                                    Log.e(LOG_TAG,"got serivceEent for " + event.getServiceId());
                                    if (event.getServiceId().equals(serviceId) &&
                                            event.getStatus().equals(SCServiceStatus.SC_SERVICE_STARTED)) {
                                        subscriber.onNext(event);
                                    }
                                }
                            });
                }
            }
        });
    }

    public void connectBackend(final SCRemoteConfig remoteConfig) {
        if (remoteConfig != null && backendService == null) {
            Log.d(LOG_TAG, "connecting backend");
            backendService = new SCBackendService(context);
            backendService.initialize(remoteConfig);
            addService(SCBackendService.serviceId(), backendService);
            startService(SCBackendService.serviceId());
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

    public SCConfigService getConfigService() {
        return configService;
    }

    public SCBackendService getBackendService() {
        return backendService;
    }

    public SCAuthService getAuthService() {
        return authService;
    }

    public SCCache getCache() {
        return cache;
    }

    public String getDeviceIdentifier() {
        String deviceId;
        SecureSharedPreferences ssp = new SecureSharedPreferences(context);
        deviceId = ssp.getString("deviceId", null);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            SecureSharedPreferences.Editor editor = ssp.edit();
            editor.putString("deviceId", deviceId);
            editor.apply();
        }
        return deviceId;
    }
}

