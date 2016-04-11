package com.boundlessgeo.spatialconnect.messaging;


import com.boundlessgeo.spatialconnect.config.SCStoreConfig;

public class SCConfigMessage extends SCMessage {

    SCStoreConfig storeConfig;

    public SCConfigMessage(MessageType type) {
        super(type);
    }

    public SCConfigMessage(MessageType type, SCStoreConfig storeConfig) {
        super(type);
        this.storeConfig = storeConfig;
    }

    public SCConfigMessage(MessageType type, String serviceId,  SCStoreConfig storeConfig) {
        super(type, serviceId);
        this.storeConfig = storeConfig;
    }

    public SCStoreConfig getStoreConfig() {
        return storeConfig;
    }
}
