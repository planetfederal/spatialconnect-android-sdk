package com.boundlessgeo.spatialconnect.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL) public class SCFormField {

  /**
   * Unique id of the form.
   */
  private String id;

  /**
   * Name for the field that can be used as a column name.
   */
  @JsonProperty("field_key") private String key;

  /**
   * A label used for the display name of this field.
   */
  @JsonProperty("field_label") private String label;

  /**
   * Boolean indicating if the field is required.
   */
  @JsonProperty("is_required") private Boolean isRequired;

  /**
   * An integer representing where the field should appear in the form.
   */
  private Integer position;

  /**
   * The type of this field.
   */
  private String type;

  /**
   * An initial value to be set for this field.
   */
  @JsonProperty("initial_value") private Object initialValue;

  /**
   * An minimum value for this field.
   */
  private Object minimum;

  /**
   * An maximum value for this field.
   */
  private Object maximum;

  /**
   * A boolean indicating if minimum is the exclusive minimum value for this field.
   *
   * @see <a href="https://github.com/json-schema/json-schema/wiki/Minimum-and-exclusiveminimum">
   * https://github.com/json-schema/json-schema/wiki/Minimum-and-exclusiveminimum</a>
   */
  @JsonProperty("exclusive_minimum") private Boolean exclusiveMinimum;

  /**
   * A boolean indicating if maximum is the exclusive maximum value for this field.
   *
   * @see <a href="https://github.com/json-schema/json-schema/wiki/Minimum-and-exclusiveminimum">
   * https://github.com/json-schema/json-schema/wiki/Minimum-and-exclusiveminimum</a>
   */
  @JsonProperty("exclusive_maximum") private Boolean exclusiveMaximum;

  /**
   * Boolean indicating if the field must be an integer.  The field must already be of type {@code
   * numeric}
   */
  @JsonProperty("is_integer") private Boolean isInteger = false;

  /**
   * An minimum length for this field.
   */
  @JsonProperty("minimum_length") private Integer minimumLength;

  /**
   * An maximum length for this field.
   */
  @JsonProperty("maximum_length") private Integer maximumLength;

  /**
   * A regex pattern that the field must match.
   */
  private String pattern;

  /**
   * A list of all possible valid values for this field.
   */
  private List<String> options;

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

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Boolean isRequired() {
    return isRequired;
  }

  public void setIsRequired(Boolean isRequired) {
    this.isRequired = isRequired;
  }

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer position) {
    this.position = position;
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

  public Boolean isInteger() {
    return isInteger;
  }

  public void setIsInteger(Boolean isInteger) {
    this.isInteger = isInteger;
  }

  public Integer getMinimumLength() {
    return minimumLength;
  }

  public void setMinimumLength(Integer minimumLength) {
    this.minimumLength = minimumLength;
  }

  public Integer getMaximumLength() {
    return maximumLength;
  }

  public void setMaximumLength(Integer maximumLength) {
    this.maximumLength = maximumLength;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public Boolean isExclusiveMinimum() {
    return exclusiveMinimum;
  }

  public void setExclusiveMinimum(Boolean exclusiveMinimum) {
    this.exclusiveMinimum = exclusiveMinimum;
  }

  public Boolean isExclusiveMaximum() {
    return exclusiveMaximum;
  }

  public void setExclusiveMaximum(Boolean exclusiveMaximum) {
    this.exclusiveMaximum = exclusiveMaximum;
  }

  public List<String> getOptions() {
    return options;
  }

  public void setOptions(List<String> options) {
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
      case "photo":
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
