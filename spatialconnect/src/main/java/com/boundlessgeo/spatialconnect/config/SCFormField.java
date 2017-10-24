package com.boundlessgeo.spatialconnect.config;

import android.text.TextUtils;

import com.boundlessgeo.spatialconnect.scutilities.Json.JsonUtilities;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;

/**
 * Utility class used to determine the Sqlite <a href="https://sqlite.org/datatype3.html">data type
 * </a> for the column used to store the form field.
 */
public class SCFormField {

    public static final String FIELD_KEY = "field_key";

    private static final String TYPE = "type";

    private static final String IS_INTEGER = "is_integer";

    private SCFormField() { /** utility classes should not have a default constructor **/ }

    public static String getColumnType(HashMap<String, Object> field) {
        String type = JsonUtilities.getString(field, TYPE);

        if (TextUtils.isEmpty(type)) {
            return "NULL";
        }

        switch (type) {
            case "string":
                return "TEXT";
            case "number":
                return JsonUtilities.getBoolean(field, IS_INTEGER) ?
                        "INTEGER" : "REAL";
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
