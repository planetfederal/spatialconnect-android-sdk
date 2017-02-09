/**
 * Copyright 2017 Boundless http://boundlessgeo.com
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
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.boundlessgeo.spatialconnect.services;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a service in the {@link SCServiceGraph}
 *
 * This class includes the service,
 * all of its dependencies,
 * and what services (recipient) relies on it.
 */
public class SCServiceNode {

    private SCService service;
    // Services this node depends on
    private List<SCServiceNode> dependencies;
    // Services this node is a dependency of
    private List<SCServiceNode> recipients;

    SCServiceNode(SCService service, List<SCServiceNode> dependencies) {
        this.recipients = new ArrayList<>();
        this.service = service;
        this.dependencies = dependencies;
    }

    public void addRecipient(SCServiceNode node) {
        recipients.add(node);
    }

    public List<SCServiceNode> getRecipients() {
        return recipients;
    }

    public SCService getService() {
        return service;
    }

    public List<SCServiceNode> getDependencies() {
        return dependencies;
    }
}
