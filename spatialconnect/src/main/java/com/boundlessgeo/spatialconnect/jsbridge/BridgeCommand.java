package com.boundlessgeo.spatialconnect.jsbridge;


public enum BridgeCommand {

    DATASERVICE_ACTIVESTORESLIST(100),
    DATASERVICE_ACTIVESTOREBYID(101),
    DATASERVICE_SPATIALQUERY(110),
    DATASERVICE_SPATIALQUERYALL(111),
    DATASERVICE_GEOSPATIALQUERY(112),
    DATASERVICE_GEOSPATIALQUERYALL(113),
    DATASERVICE_CREATEFEATURE(114),
    DATASERVICE_UPDATEFEATURE(115),
    DATASERVICE_DELETEFEATURE(116),
    SENSORSERVICE_GPS(200);

    private final int actionNumber;

    BridgeCommand(int actionNumber) {
        this.actionNumber = actionNumber;
    }

    public static BridgeCommand fromActionNumber(int actionNumber) {
        for (BridgeCommand v : values()) {
            if (v.actionNumber == actionNumber) {
                return v;
            }
        }
        throw new IllegalArgumentException(
                String.valueOf(actionNumber) + " is not an action number associated with a BridgeCommand."
        );
    }

}
