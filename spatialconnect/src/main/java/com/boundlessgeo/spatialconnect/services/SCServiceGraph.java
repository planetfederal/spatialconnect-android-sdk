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

    public SCServiceGraph() {
        this.serviceEventSubject = PublishSubject.create();
        this.serviceEvents = serviceEventSubject.publish();
    }

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
        HashMap<String, SCServiceNode> sn  = new HashMap<>(servicesNodes);
        for (Object value : sn.values()) {
            String serviceId = ((SCServiceNode) value).getService().getId();
            boolean started = startService(serviceId);
            if (started) {
                serviceEventSubject.onNext(
                        new SCServiceStatusEvent(SCServiceStatus.SC_SERVICE_RUNNING, serviceId));
            } else {
                serviceEventSubject.onNext(
                        new SCServiceStatusEvent(SCServiceStatus.SC_SERVICE_ERROR, serviceId));
            }
        }
    }

    public boolean startService(String serviceId) {
        Log.e(LOG_TAG, "Starting ..." + serviceId);
        SCServiceNode node = getNodeById(serviceId);

        //check to see if running
        if (node.getService().getStatus() == SCServiceStatus.SC_SERVICE_RUNNING) {
            return true;
        }

        List<Boolean> depsStarts = new ArrayList<>();
        SCService svc;
        for (SCServiceNode serviceNodeDep : node.getDependencies()) {
            svc = serviceNodeDep.getService();
            if (svc.getStatus() == SCServiceStatus.SC_SERVICE_RUNNING) {
                depsStarts.add(true);
            } else {
                depsStarts.add(startService(svc.getServiceId()));
            }
        }

        //No Deps, just start
        if (depsStarts.size() == 0) {
            return node.getService().start(null);
        } else {
            if (node.getDependencies().size() != depsStarts.size()) {
                Log.e(LOG_TAG, "Not all of the dependencies started");
                return false;
            }

            Map<String, SCService> deps = new HashMap<>();
            for (SCServiceNode dep : node.getDependencies()) {
                deps.put(dep.getService().getId(), dep.getService());
            }

            return node.getService().start(deps);
        }
    }

    public void stopAllServices() {
        HashMap<String, SCServiceNode> sn  = new HashMap<>(servicesNodes);
        for (Object value : sn.values()) {
            String serviceId = ((SCServiceNode) value).getService().getId();
            boolean stopped = stopService(serviceId);
            if (stopped) {
                serviceEventSubject.onNext(
                        new SCServiceStatusEvent(SCServiceStatus.SC_SERVICE_STOPPED, serviceId));
            } else {
                serviceEventSubject.onNext(
                        new SCServiceStatusEvent(SCServiceStatus.SC_SERVICE_ERROR, serviceId));
            }
        }
    }

    public boolean stopService(String serviceId) {
        SCServiceNode node = getNodeById(serviceId);

        //check to see if running
        if (node.getService().getStatus() == SCServiceStatus.SC_SERVICE_STOPPED) {
            return true;
        }

        List<Boolean> depsStops = new ArrayList<>();
        SCService svc;
        for (SCServiceNode serviceNodeDep : node.getDependencies()) {
            svc = serviceNodeDep.getService();
            if (svc.getStatus() == SCServiceStatus.SC_SERVICE_STOPPED) {
                depsStops.add(true);
            } else {
                depsStops.add(stopService(svc.getServiceId()));
            }
        }

        if (depsStops.size() > 0) {
            return node.getService().stop();
        } else {
            if (node.getDependencies().size() != depsStops.size()) {
                Log.e(LOG_TAG, "Not all of the dependencies stopped");
                return false;
            } else {
                return node.getService().stop();
            }
        }
    }
}
