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
package com.boundlessgeo.spatialconnect.services;

import android.util.Log;

import java.util.List;
import java.util.Map;

public abstract class SCService implements SCServiceLifecycle {
    private SCServiceStatus status;
    private String serviceId;

    public SCService() {
        this.status = SCServiceStatus.SC_SERVICE_STOPPED;
    }

    @Override
    public boolean start(Map<String, SCService> deps) {
        Log.v(this.getClass().getSimpleName(), "Starting service " + getServiceId());
        this.status = SCServiceStatus.SC_SERVICE_RUNNING;
        return true;
    }

    public boolean stop() {
        this.status = SCServiceStatus.SC_SERVICE_STOPPED;
        return true;
    }

    public boolean resume() {
        this.status = SCServiceStatus.SC_SERVICE_RUNNING;
        return true;
    }

    public boolean pause() {
        this.status = SCServiceStatus.SC_SERVICE_PAUSED;
        return true;
    }

    @Override
    public void startError() {
        this.status = SCServiceStatus.SC_SERVICE_ERROR;
    }

    public SCServiceStatus getStatus() {
        return status;
    }

    public void setStatus(SCServiceStatus status) {
        this.status = status;
    }

    public String getServiceId() {
        return getId();
    }

    public abstract String getId();

    public abstract List<String> getRequires();
}
