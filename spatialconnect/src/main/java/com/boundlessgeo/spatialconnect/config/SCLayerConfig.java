package com.boundlessgeo.spatialconnect.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.UUID;

public class SCLayerConfig {

    /**
     * Unique id of the form.
     */
    @JsonProperty("id")
    private UUID id;

    /**
     * Immutable name of the form.
     */
    @JsonProperty("layer_key")
    private String layerKey;

    /**
     * The label to display for the form.
     */
    @JsonProperty("layer_label")
    private String layerLabel;

    /**
     * The version of this form.
     */
    @JsonProperty("version")
    private String version;


    @JsonProperty("schema")
    private SCLayerSchema schema;

    /**
     * Unique id of the team to which this form belongs.
     */
    @JsonProperty("team_id")
    private String teamId;

    /**
     * Metadata about this form config.
     */
    @JsonProperty("metadata")
    private LayerMetadata metadata;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SCLayerSchema getSchema() {
        return schema;
    }

    public void setSchema(SCLayerSchema schema) {
        this.schema = schema;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLayerKey() {
        return layerKey;
    }

    public void setLayerKey(String formKey) {
        this.layerKey = formKey;
    }

    public String getLayerLabel() {
        return layerLabel;
    }

    public void setLayerLabel(String layerLabel) {
        this.layerLabel = layerLabel;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public LayerMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(LayerMetadata metadata) {
        this.metadata = metadata;
    }

    public HashMap<String, Object> toJSON() {
        HashMap<String, Object> json = new HashMap<>();

        json.put("id", getId());
        json.put("layer_key", getLayerKey());  // same as layer name
        json.put("layer_label", getLayerLabel());
        json.put("version", getVersion());
        json.put("schema", schema.getFields());

        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SCLayerConfig that = (SCLayerConfig) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public class LayerMetadata {

        private Integer count;

        @JsonProperty("lastActivity")
        private String lastActivity;

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public String getLastActivity() {
            return lastActivity;
        }

        public void setLastActivity(String lastActivity) {
            this.lastActivity = lastActivity;
        }
    }
}


