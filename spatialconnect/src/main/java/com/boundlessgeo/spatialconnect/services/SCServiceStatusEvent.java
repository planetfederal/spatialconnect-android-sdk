package com.boundlessgeo.spatialconnect.services;

/**
 * Created by Landon on 12/14/16.
 */

public class SCServiceStatusEvent {
    private SCServiceStatus status;
    private String serviceId;

    public SCServiceStatusEvent(SCServiceStatus status, String serviceId) {
        this.status = status;
        this.serviceId = serviceId;
    }

    public SCServiceStatusEvent(SCServiceStatus status) {
        this.status = status;
    }

    public SCServiceStatus getStatus() {
        return this.status;
    }

    public String getServiceId() {
        return this.serviceId;
    }
}
