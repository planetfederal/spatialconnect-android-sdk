package com.boundlessgeo.spatialconnect.stores;

public class SCDataStoreException extends RuntimeException {

    private ExceptionType type;

    public SCDataStoreException() {
        super();
    }

    public SCDataStoreException(String message) {
        super(message);
    }

    public SCDataStoreException(ExceptionType type, String message) {
        super(message);
        this.type = type;
    }

    public ExceptionType getType() {
        return type;
    }

    public void setType(ExceptionType type) {
        this.type = type;
    }

    public enum ExceptionType {
        LAYER_NOT_FOUND
    }
}
