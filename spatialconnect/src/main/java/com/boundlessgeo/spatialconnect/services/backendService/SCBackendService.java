/**
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
package com.boundlessgeo.spatialconnect.services.backendService;


import android.content.Context;

import com.boundlessgeo.spatialconnect.mqtt.SCNotification;
import com.boundlessgeo.spatialconnect.services.SCConfigService;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCService;
import com.boundlessgeo.spatialconnect.services.SCServiceLifecycle;
import com.boundlessgeo.spatialconnect.services.authService.SCAuthService;

import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.subjects.BehaviorSubject;

import static java.util.Arrays.asList;

public class SCBackendService extends SCService implements SCServiceLifecycle {

    private static final String SERVICE_NAME = "SC_BACKEND_SERVICE";
    private Context context;
    private ISCBackend scBackend;

    /**
     * BehaviorSubject that emits True when the SCConfig has been received
     */
    protected static BehaviorSubject<Boolean> configReceived = BehaviorSubject.create(false);

    public SCBackendService(Context context, ISCBackend scBackend) {
        this.context = context;
        this.scBackend = scBackend;
    }

    @Override
    public boolean start(Map<String, SCService> deps) {
        boolean started = super.start(deps);
        scBackend.start(deps);
        return started;
    }

    @Override
    public String getId() {
        return SERVICE_NAME;
    }

    public static String serviceId() {
        return SERVICE_NAME;
    }

    @Override
    public List<String> getRequires() {
        return asList(SCAuthService.serviceId(), SCSensorService.serviceId(), SCDataService.serviceId(),
                SCConfigService.serviceId());
    }

    public ISCBackend getSCBackend() {
        return scBackend;
    }

    public void updateDeviceToken(String token) {
        scBackend.updateDeviceToken(token);
    }

    public Observable<SCNotification> getNotifications() {
        return scBackend.getNotifications();
    }

    public String getBackendUri() {
        return scBackend.getBackendUri();
    }
}
