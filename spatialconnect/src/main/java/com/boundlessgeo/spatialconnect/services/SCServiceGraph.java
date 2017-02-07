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

package com.boundlessgeo.spatialconnect.services ;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.observables.ConnectableObservable;
import rx.subjects.PublishSubject;

public class SCServiceGraph {
    private static String LOG_TAG = SCServiceGraph.class.getSimpleName();
    private Map<String, SCServiceNode> servicesNodes = new HashMap<>();
    public PublishSubject<SCServiceStatusEvent> serviceEventSubject;
    public ConnectableObservable<SCServiceStatusEvent> serviceEvents;

    public void addServivce(SCService service) {
        List<String> requires = service.getRequires();
        List<SCServiceNode> deps = new ArrayList<>();
        if (requires != null) {
            for (String svc: requires) {
                deps.add(getNodeById(svc));
            }
        }

        SCServiceNode serviceNode = new SCServiceNode(service, deps);
        if (deps.size() > 0) {
            for (SCServiceNode recipent: deps) {
                recipent.addRecipient(serviceNode);
            }
        }

        servicesNodes.put(service.getId(), serviceNode);
    }

    public void removeService(String serviceId) {
        SCServiceNode serviceNode = getNodeById(serviceId);
        if (serviceNode == null) {
            return;
        }

        if (serviceNode.getRecipients().size() > 0) {
            for (SCServiceNode deps: serviceNode.getRecipients()) {
                removeService(deps.getService().getServiceId());
            }
        }

        servicesNodes.remove(serviceId);
    }

    public SCServiceNode getNodeById(String serviceId) {
        return servicesNodes.get(serviceId);
    }

    public void startAllServices() {
        for (Object value: servicesNodes.values()) {
            startService(((SCServiceNode) value).getService().getId());
        }
    }

    public void startService(String serviceId) {
        Log.e(LOG_TAG, "Starting ..." + serviceId);
        SCServiceNode node = getNodeById(serviceId);

//        if (node.getDependencies().size() > 0) {
//            for (SCServiceNode dep: node.getDependencies()) {
//                SCService service = dep.getService();
//                checkAndStartService(service);
//            }
//            checkAndStartService(node.getService());
//        } else {
//            checkAndStartService(node.getService());
//        }

        for (SCServiceNode dep: node.getDependencies()) {
            SCService service = dep.getService();
            //grap dep for dep service
            Map<String, SCService> deps = new HashMap<>();
            for (SCServiceNode serviceNodeDep: dep.getDependencies()) {
                deps.put(serviceNodeDep.getService().getId(), serviceNodeDep.getService());
            }
            checkAndStartService(service, deps);
        }
        //after all dependencies and or no dependencies, start the service
        checkAndStartService(node.getService(), null);


    }

    public void stopAllServices() {

    }

    public void stopService(String serviceId) {

    }

    public void restartAllServices() {

    }

    private void checkAndStartService(SCService service, Map<String, SCService> deps) {
        if (service.getStatus() != SCServiceStatus.SC_SERVICE_RUNNING) {
            service.start(deps);
        }
    }
}
