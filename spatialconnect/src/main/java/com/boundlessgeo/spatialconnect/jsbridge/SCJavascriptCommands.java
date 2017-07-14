package com.boundlessgeo.spatialconnect.jsbridge;

public class SCJavascriptCommands {

    public enum SCBridgeStatus {
        NEXT(0), COMPLETED(1), ERROR(2);

        private final int mStatus;

        SCBridgeStatus(int status) {
            mStatus = status;
        }

        public int statusValue() {
            return mStatus;
        }
    }
}
