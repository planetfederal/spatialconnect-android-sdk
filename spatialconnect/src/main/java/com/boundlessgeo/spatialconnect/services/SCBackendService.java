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
package com.boundlessgeo.spatialconnect.services;


import com.boundlessgeo.spatialconnect.config.SCRemoteConfig;

public abstract class SCBackendService extends SCService implements SCServiceLifecycle {

    private static final String SERVICE_NAME = "SC_BACKEND_SERVICE";

    public static String serviceId() {
        return SERVICE_NAME;
    }

    abstract public void initialize(SCRemoteConfig config);

    abstract public String getBackendUri();

}
