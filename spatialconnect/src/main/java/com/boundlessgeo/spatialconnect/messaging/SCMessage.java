package com.boundlessgeo.spatialconnect.messaging;

public class SCMessage {

    private MessageType type;
    private String serviceId;

    public SCMessage(MessageType type) {
        this.type = type;
    }

    public SCMessage(MessageType type, String serviceId) {
        this.type = type;
        this.serviceId = serviceId;
    }

    public MessageType getType() {
        return type;
    }

    public String getServiceId() {
        return serviceId;
    }

}
