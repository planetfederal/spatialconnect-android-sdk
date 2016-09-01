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

import java.util.UUID;

public class SCService {
    private String id;
    private SCServiceStatus status;

    public SCService() {
        this.id = UUID.randomUUID().toString();
        this.status = SCServiceStatus.SC_SERVICE_STOPPED;
    }

    // TODO: should this return an observable?
    public void start() {
        this.status = SCServiceStatus.SC_SERVICE_STARTING;
    }

    public void stop() {
        this.status = SCServiceStatus.SC_SERVICE_STOPPED;
    }

    public void resume() {
        this.status = SCServiceStatus.SC_SERVICE_RUNNING;
    }

    public void pause() {
        this.status = SCServiceStatus.SC_SERVICE_PAUSED;
    }

    public String getId() {
        return id;
    }

    public SCServiceStatus getStatus() {
        return status;
    }

    public void setStatus(SCServiceStatus status) {
        this.status = status;
    }
}
