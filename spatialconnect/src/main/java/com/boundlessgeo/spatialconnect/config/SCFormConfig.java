package com.boundlessgeo.spatialconnect.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SCFormConfig {

    /**
     * Unique id of the form.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Name of the form.
     */
    @JsonProperty("name")
    private String name;

    /**
     * List of the form fields that define this form.
     */
    @JsonProperty("fields")
    private List<SCFormField> fields;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SCFormField> getFields() {
        return fields;
    }

    public void setFields(List<SCFormField> fields) {
        this.fields = fields;
    }

}

