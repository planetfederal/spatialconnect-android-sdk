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

import java.util.HashMap;
import java.util.UUID;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subjects.PublishSubject;

import static android.R.attr.id;

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
    public PublishSubject<SCServiceStatusEvent> serviceEventSubject;
    public ConnectableObservable<SCServiceStatusEvent> serviceEvents;

    private SpatialConnect() {
        this.serviceEventSubject = PublishSubject.create();
        this.serviceEvents = serviceEventSubject.publish();
        this.services = new HashMap<>();
    }

    /** Singleton of SpatialConnect
     *
     * @return instance of the SpatialConnect object singleton
     */
    public static SpatialConnect getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Sets up the SpatialConnect instance that is the entry point of the library
     *
     * @param context
     */
    public void initialize(Context context) {
        Log.d(LOG_TAG, "Initializing SpatialConnect");

        this.sensorService = new SCSensorService(context);
        this.dataService = new SCDataService(context);
        this.configService = new SCConfigService(context);
        this.authService = new SCAuthService(context);
        this.cache = new SCCache(context);
        this.context = context;
        addDefaultServices();
    }

    /**
     * Adds a {@link SCService} to SpatialConnect
     * @param service the service object
     */
    public void addService(SCService service) {
        this.services.put(service.getServiceId(), service);
    }

    /**
     * Removes a service
     * @param serviceId the id of the service to be removed
     */
    public void removeService(String serviceId) {
        this.services.remove(serviceId);
    }

    /**
     * Starts {@link SCService} based on service id
     * @param serviceId the id of the service that needs to start
     */
    public void startService(final String serviceId) {
        this.services.get(id).start().subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {}
            },
                new Action1<Throwable>() {
                @Override
                public void call(Throwable t) {
                    Log.d(LOG_TAG, t.getLocalizedMessage());
                    // onError can happen if we cannot start the service b/c of some error or runtime exception
                    serviceEventSubject.onNext(
                            new SCServiceStatusEvent(SCServiceStatus.SC_SERVICE_ERROR, serviceId)
                    );
                }
            }, new Action0() {
                @Override
                public void call() {
                    serviceEventSubject.onNext(
                            new SCServiceStatusEvent(SCServiceStatus.SC_SERVICE_RUNNING, serviceId));
                }
        });
    }

    /**
     * Starts all services added to SpatialConnect
     */
    public void startAllServices() {
        Log.d(LOG_TAG, "Starting all services.");
        HashMap<String, SCService> ss  = new HashMap<>(services);
        for (String key : ss.keySet()) {
            startService(key);
        }
    }

    /**
     * Stops an SCService based on service id
     * @param serviceId the id of the service that needs to be stopped.
     */
    public void stopService(String serviceId) {
        this.services.get(serviceId).stop();
    }

    /**
     * Stops all SCServices that was added to SpatialConnect
     */
    public void stopAllServices() {
        for (String key : this.services.keySet()) {
            this.services.get(key).stop();
        }
    }

    /**
     * Restarts SCService based on service id
     * @param serviceId the id of the service that needs to be restarted.
     */
    public void restartService(String serviceId) {
        this.services.get(serviceId).stop();
        this.services.get(serviceId).start();
    }

    /**
     * Restarts all SCServices taht was added to SpatialConnect
     */
    public void restartAllServices() {
        for (String key : this.services.keySet()) {
            this.services.get(key).stop();
            this.services.get(key).start();
        }
    }

    /**
     * Finds service from service id
     * @param serviceId the id of the service that needs to be retrieved
     * @return instance of service found by id
     */
    public SCService getServiceById(String serviceId) {
        return this.services.get(serviceId);
    }

    /**
     * Checks to see if an {@link SCService}  is running by sending {@link SCServiceStatusEvent }
     * @param serviceId the id of the service that needs to be retrieved
     * @return Observable of SCServiceStatusEvent
     */
    public Observable<SCServiceStatusEvent> serviceRunning(final String serviceId) {
        SCService service = getServiceById(serviceId);
        if (service != null && service.getStatus() == SCServiceStatus.SC_SERVICE_RUNNING) {
            return Observable.just(new SCServiceStatusEvent(SCServiceStatus.SC_SERVICE_RUNNING));
        } else {
            return serviceEvents.autoConnect()
                    .filter(new Func1<SCServiceStatusEvent, Boolean>() {
                        @Override
                        public Boolean call(SCServiceStatusEvent scServiceStatusEvent) {
                            return scServiceStatusEvent.getServiceId().equals(serviceId) &&
                                    scServiceStatusEvent.getStatus().equals(SCServiceStatus.SC_SERVICE_RUNNING);
                        }
                    });
        }
    }

    /**
     * Creates {@link SCBackendService} and connects via the SCRemoteConfig
     * @param remoteConfig object that represents http and mqtt endpoints.
     */
    public void connectBackend(final SCRemoteConfig remoteConfig) {
        if (remoteConfig != null && backendService == null) {
            Log.d(LOG_TAG, "connecting backend");
            backendService = new SCBackendService(context);
            backendService.initialize(remoteConfig);
            addService(backendService);
            startService(backendService.getServiceId());
        }
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

    /**
     * Adds all the default services to the services hash map maintained by the SpatialConnect.
     */
    private void addDefaultServices() {
        addService(this.dataService);
        addService(this.sensorService);
        addService(this.configService);
        addService(this.authService);
    }

    private static class SingletonHelper {
        private static final SpatialConnect INSTANCE = new SpatialConnect();
    }
}

