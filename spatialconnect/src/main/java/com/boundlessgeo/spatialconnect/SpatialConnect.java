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
import com.boundlessgeo.spatialconnect.services.SCBackendService;
import com.boundlessgeo.spatialconnect.services.SCConfigService;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCService;
import com.boundlessgeo.spatialconnect.services.SCServiceGraph;
import com.boundlessgeo.spatialconnect.services.SCServiceNode;
import com.boundlessgeo.spatialconnect.services.SCServiceStatus;
import com.boundlessgeo.spatialconnect.services.SCServiceStatusEvent;
import com.boundlessgeo.spatialconnect.services.authService.ISCAuth;
import com.boundlessgeo.spatialconnect.services.authService.SCAuthService;
import com.github.rtoshiro.secure.SecureSharedPreferences;

import java.util.UUID;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * When instantiated, SpatialConnect adds the default services and registers all stores
 * defined in the config file.  SpatialConnect is also used to manage the services (starting,
 * stopping, creating, deleting, etc).
 */
public class SpatialConnect {

    private final String LOG_TAG = SpatialConnect.class.getSimpleName();
    private SCServiceGraph serviceGraph = new SCServiceGraph();
    private SCDataService dataService;
    private SCSensorService sensorService;
    private SCConfigService configService;
    private SCBackendService backendService;
    private SCAuthService authService;
    private SCCache cache;

    private Context context;

    private SpatialConnect() {}

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
        this.cache = new SCCache(context);
        this.context = context;
        addDefaultServices();
    }

    /**
     * Adds an instantiated instance of Service that extends the {@link SCService} class.
     * The 'service' must extend the {@link SCService} class.
     * @param service the service object
     */
    public void addService(SCService service) {
        serviceGraph.addServivce(service);
    }

    /**
     * This stops and removes a {@link SCService} from the SpatialConnect instance.
     * @param serviceId the id of the service to be removed
     */
    public void removeService(String serviceId) {
        serviceGraph.removeService(serviceId);
    }

    /**
     * This is the preferred way to start a service.
     *
     * Do not call service start on the service instance. Use this method to
     * start a {@link SCService}.
     * @param serviceId the id of the service that needs to start
     */
    public void startService(final String serviceId) {
        serviceGraph.startService(serviceId);
    }

    /**
     * This starts all the services in the order they were added. Data,
     * Sensor, Config, and Auth are all started. Backend Service waits for the Config
     * service to find a remote backend.
     */
    public void startAllServices() {
        Log.d(LOG_TAG, "Starting all services.");
        serviceGraph.startAllServices();
    }

    /**
     * This is the preferred way to stop a service.
     *
     * Do not call service stop on the service instance. Use this method to
     * stop a {@link SCService}.
     * @param serviceId the id of the service that needs to be stopped.
     */
    public void stopService(String serviceId) {
        serviceGraph.stopService(serviceId);
    }

    /**
     * Stops all the services in the order they were added to the services.
     */
    public void stopAllServices() {
        serviceGraph.stopAllServices();
    }

    /**
     * This is the preferred way to restart a service.
     *
     * Restarts a single service
     *
     * Do not call service restart on the service instance. Use this method
     * to start a {@link SCService}.
     * @param serviceId the id of the service that needs to be restarted.
     */
    public void restartService(String serviceId) {
        serviceGraph.stopService(serviceId);
        serviceGraph.startService(serviceId);
    }

    /**
     * Restarts all SCServices taht was added to SpatialConnect
     */
    public void restartAllServices() {
        serviceGraph.restartAllServices();
    }

    /**
     * Finds service from service id
     * @param serviceId the id of the service that needs to be retrieved
     * @return instance of service found by id
     */
    public SCService getServiceById(String serviceId) {
        SCServiceNode serviceNode = serviceGraph.getNodeById(serviceId);
        if (serviceNode != null) {
            return serviceNode.getService();
        } else {
            return null;
        }
    }

    /**
     * Emits an {@link SCServiceStatusEvent } when the service is running. If the
     * service isn't started, this will wait until it is started. This can be used by
     * your app to start wiring up functionality waiting for it to occur.
     *
     * This is the best way to know if a {@link SCService}  is started. If the service is already started, it will
     * return an event immediately. You can also receive errors in the subscribe's
     * error block. The observable will complete when the store is confirmed to have started.
     *
     * @param serviceId the id of the service that needs to be retrieved
     * @return Observable of SCServiceStatusEvent
     */
    public Observable<SCServiceStatusEvent> serviceRunning(final String serviceId) {
        SCService service = getServiceById(serviceId);
        if (service != null && service.getStatus() == SCServiceStatus.SC_SERVICE_RUNNING) {
            return Observable.just(new SCServiceStatusEvent(SCServiceStatus.SC_SERVICE_RUNNING));
        } else {
            return serviceGraph.serviceEvents.autoConnect()
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
     * If you have an instance of SpatialConnect Server, this is how you would register it.
     * Passing in a remote configuration object will use the info to start the connection to the backend.
     * @param remoteConfig object that represents http and mqtt endpoints {@link SCRemoteConfig}.
     */
    public void connectBackend(final SCRemoteConfig remoteConfig) {
        if (serviceGraph.getNodeById(SCBackendService.serviceId()) == null) {
            backendService = new SCBackendService(context);
            backendService.initialize(remoteConfig);
            addService(backendService);
            startService(backendService.getServiceId());
        }
    }

    public void connectAuth(ISCAuth authMethod) {
        if (serviceGraph.getNodeById(SCAuthService.serviceId()) == null) {
            authService = new SCAuthService(context, authMethod);
            addService(authService);
            startService(SCAuthService.serviceId());
        } else {
            Log.d(LOG_TAG, "SCAuthService Already Connected");
        }
    }

    public void updateDeviceToken(final String token) {

        serviceRunning(SCBackendService.serviceId()).subscribe(
                new Action1<SCServiceStatusEvent>() {
                    @Override
                    public void call(SCServiceStatusEvent scServiceStatusEvent) {}
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable t) {}
                }, new Action0() {
                    @Override
                    public void call() {
                        backendService.updateDeviceToken(token);
                    }
                });
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

    /**
     * Identifies an application instance running on a device. This is the recommended
     * solution for non-ads use-cases.
     * @return String UUID string of the install id
     */
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
        addService(this.sensorService);
        addService(this.dataService);
        addService(this.configService);
    }

    private static class SingletonHelper {
        private static final SpatialConnect INSTANCE = new SpatialConnect();
    }
}

