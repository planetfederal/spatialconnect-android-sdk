package com.boundlessgeo.spatialconnect.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SCFormField {

    /**
     * Unique id of the form.
     */
    private String id;

    /**
     * Name for the field that can be used as a column name.
     */
    private String key;

    /**
     * A label used for the display name of this field.
     */
    private String name;

    /**
     * Boolean indicating if the field is required.
     */
    @JsonProperty("is_required")
    private Boolean isRequired;

    /**
     * An integer representing where the field should appear in the form.
     */
    private Integer order;

    /**
     * The type of this field.
     */
    private String type;

    /**
     * An initial value to be set for this field.
     */
    @JsonProperty("initial_value")
    private Object initialValue;

    /**
     * An minimum value for this field.
     */
    private Object minimum;

    /**
     * An maximum value for this field.
     */
    private Object maximum;

    /**
     * An exlusive minimum value for this field.
     * @see <a href="https://github.com/json-schema/json-schema/wiki/Minimum-and-exclusiveminimum">
     *     https://github.com/json-schema/json-schema/wiki/Minimum-and-exclusiveminimum</a>
     */
    @JsonProperty("exclusive_minimum")
    private Object exclusiveMinimum;

    /**
     * An exclusive maximum value for this field.
     * @see <a href="https://github.com/json-schema/json-schema/wiki/Minimum-and-exclusiveminimum">
     *     https://github.com/json-schema/json-schema/wiki/Minimum-and-exclusiveminimum</a>
     */
    @JsonProperty("exclusive_maximum")
    private Object exclusiveMaximum;

    /**
     * Boolean indicating if the field must be an integer.  The field must already be of type {@code numeric}
     */
    @JsonProperty("is_integer")
    private Boolean isInteger;

    /**
     * An minimum length for this field.
     */
    private Object minimumLength;

    /**
     * An maximum length for this field.
     */
    private Object maximumLength;

    /**
     * A regex pattern that the field must match.
     */
    private String pattern;

    /**
     * A list of all possible valid values for this field.
     */
    private List<Object> options;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(Object initialValue) {
        this.initialValue = initialValue;
    }

    public Object getMinimum() {
        return minimum;
    }

    public void setMinimum(Object minimum) {
        this.minimum = minimum;
    }

    public Object getMaximum() {
        return maximum;
    }

    public void setMaximum(Object maximum) {
        this.maximum = maximum;
    }

    public Object getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public void setExclusiveMinimum(Object exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public Object getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public void setExclusiveMaximum(Object exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public Boolean isInteger() {
        return isInteger;
    }

    public void setIsInteger(Boolean isInteger) {
        this.isInteger = isInteger;
    }

    public Object getMinimumLength() {
        return minimumLength;
    }

    public void setMinimumLength(Object minimumLength) {
        this.minimumLength = minimumLength;
    }

    public Object getMaximumLength() {
        return maximumLength;
    }

    public void setMaximumLength(Object maximumLength) {
        this.maximumLength = maximumLength;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<Object> getOptions() {
        return options;
    }

    public void setOptions(List<Object> options) {
        this.options = options;
    }

    public String getColumnType() {
        switch (this.type) {
            case "string":
                return "TEXT";
            case "number":
                if (isInteger) {
                    return "INTEGER";
                }
                return "REAL";
            case "boolean":
                return "INTEGER";
            case "date":
                return "TEXT";
            case "slider":
                return "TEXT";
            case "counter":
                return "INTEGER";
            case "select":
                return "TEXT";
            default:
                return "NULL";
        }
    }
}
