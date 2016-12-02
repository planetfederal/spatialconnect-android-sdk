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
     * Immutable name of the form.
     */
    @JsonProperty("form_key")
    private String formKey;

    /**
     * The label to display for the form.
     */
    @JsonProperty("form_label")
    private String formLabel;

    /**
     * The version of this form.
     */
    @JsonProperty("version")
    private String version;

    /**
     * List of the form fields that define this form.
     */
    @JsonProperty("fields")
    private List<SCFormField> fields;

    /**
     * Unique id of the team to which this form belongs.
     */
    @JsonProperty("team_id")
    private String teamId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<SCFormField> getFields() {
        return fields;
    }

    public void setFields(List<SCFormField> fields) {
        this.fields = fields;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    public String getFormLabel() {
        return formLabel;
    }

    public void setFormLabel(String formLabel) {
        this.formLabel = formLabel;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
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

}


