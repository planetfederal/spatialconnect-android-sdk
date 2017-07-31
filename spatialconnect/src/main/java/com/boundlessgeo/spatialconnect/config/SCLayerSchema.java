package com.boundlessgeo.spatialconnect.config;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;

public class SCLayerSchema {

    @JsonProperty("fields")
    private List<HashMap<String, Object>> fields;

    public List<HashMap<String, Object>> getFields() {
        return fields;
    }

    public void setFields(List<HashMap<String, Object>> fields) {
        this.fields = fields;
    }
}
