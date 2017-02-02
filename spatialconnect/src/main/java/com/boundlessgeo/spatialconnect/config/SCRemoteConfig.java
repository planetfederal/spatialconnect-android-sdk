/**
 * Copyright 2016 Boundless, http://boundlessgeo.com
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
package com.boundlessgeo.spatialconnect.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SCRemoteConfig {

    @JsonProperty("http_protocol")
    private String httpProtocol;

    @JsonProperty("http_host")
    private String httpHost;

    @JsonProperty("http_port")
    private Integer httpPort;

    @JsonProperty("mqtt_protocol")
    private String mqttProtocol;

    @JsonProperty("mqtt_host")
    private String mqttHost;

    @JsonProperty("mqtt_port")
    private Integer mqttPort;

    public String getHttpProtocol() {
        return httpProtocol;
    }

    public void setHttpProtocol(String httpProtocol) {
        this.httpProtocol = httpProtocol;
    }

    public String getHttpHost() {
        return httpHost;
    }

    public void setHttpHost(String httpHost) {
        this.httpHost = httpHost;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public String getMqttProtocol() {
        return mqttProtocol;
    }

    public void setMqttProtocol(String mqttProtocol) {
        this.mqttProtocol = mqttProtocol;
    }

    public String getMqttHost() {
        return mqttHost;
    }

    public void setMqttHost(String mqttHost) {
        this.mqttHost = mqttHost;
    }

    public Integer getMqttPort() {
        return mqttPort;
    }

    public void setMqttPort(Integer mqttPort) {
        this.mqttPort = mqttPort;
    }

    public String getHttpUri() {
        return String.format(Locale.US,"%s://%s:%s", httpProtocol, httpHost, httpPort);
    }
}
