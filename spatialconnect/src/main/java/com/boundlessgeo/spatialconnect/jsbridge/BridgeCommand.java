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
