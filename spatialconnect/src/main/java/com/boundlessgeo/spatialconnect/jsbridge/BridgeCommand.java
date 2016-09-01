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

    START_ALL_SERVICES(1),
    DATASERVICE_ACTIVESTORESLIST(100),
    DATASERVICE_ACTIVESTOREBYID(101),
    DATASERVICE_SPATIALQUERY(110),
    DATASERVICE_SPATIALQUERYALL(111),
    DATASERVICE_GEOSPATIALQUERY(112),
    DATASERVICE_GEOSPATIALQUERYALL(113),
    DATASERVICE_CREATEFEATURE(114),
    DATASERVICE_UPDATEFEATURE(115),
    DATASERVICE_DELETEFEATURE(116),
    DATASERVICE_FORMLIST(117),
    SENSORSERVICE_GPS(200),
    AUTHSERVICE_AUTHENTICATE(300),
    AUTHSERVICE_LOGOUT(301),
    AUTHSERVICE_ACCESS_TOKEN(302),
    AUTHSERVICE_LOGIN_STATUS(303),
    NETWORKSERVICE_GET_REQUEST(400),
    NETWORKSERVICE_POST_REQUEST(401),
    CONFIG_FULL(500),
    CONFIG_STORE_LIST(501),
    CONFIG_ADD_STORE(502),
    CONFIG_REMOVE_STORE(503),
    CONFIG_UPDATE_STORE(504),
    CONFIG_FORM_LIST(505),
    CONFIG_ADD_FORM(506),
    CONFIG_REMOVE_FORM(507),
    CONFIG_UPDATE_FORM(508),
    CONFIG_REGISTER_DEVICE(509),
    NOTIFICATIONS(600),
    NOTIFICATION_ALERT(601),
    NOTIFICATION_INFO(602),
    NOTIFICATION_CONTENT_AVAILABLE(602);

    private final int actionNumber;

    BridgeCommand(int actionNumber) {
        this.actionNumber = actionNumber;
    }

    public int value() {
        return actionNumber;
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
