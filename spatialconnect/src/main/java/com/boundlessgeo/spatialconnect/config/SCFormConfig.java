package com.boundlessgeo.spatialconnect.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SCFormConfig that = (SCFormConfig) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @JsonProperty("layer_name")
    public String getLayerName() {
        return name.replace(" ", "_").toLowerCase();
    }

    @JsonProperty("display_name")
    public String getDisplayName() {
        return name;
    }
}

