/**
 * Copyright 2015-2017 Boundless, http://boundlessgeo.com
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

package com.boundlessgeo.spatialconnect.style;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class SCStyle {

    private String LOG_TAG = SCStyle.class.getSimpleName();
    private String fillColor = null;
    private float fillOpacity = 0;
    private String strokeColor = null;
    private float strokeOpacity = 0;
    private String iconColor = null;

    public SCStyle() {}

    public SCStyle(ArrayNode mapBoxStyle) {
        try {
            for (int i = 0; i < mapBoxStyle.size(); i++) {
                JsonNode style = mapBoxStyle.get(i);
                JsonNode paint = style.get("paint");
                fillColor = paint.get("fill-color").textValue();
                fillOpacity = paint.get("fill-opacity").floatValue();
                strokeColor = paint.get("line-color").textValue();
                strokeOpacity = paint.get("line-opacity").floatValue();
                iconColor = paint.get("icon-color").textValue();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error parsing mapbox style" + e.getLocalizedMessage());
        }
    }

    public String getFillColor() {
        if (fillColor == null) {
            return "#000000";
        }
        return fillColor;
    }

    public float getFillOpacity() {
        if (fillOpacity == 0) {
            return 0.5f;
        }
        return fillOpacity;
    }

    public String getStrokeColor() {
        if (strokeColor == null) {
            return "#000000";
        }
        return strokeColor;
    }

    public float getStrokeOpacity() {
        if (strokeOpacity == 0) {
            return 0.5f;
        }
        return strokeOpacity;
    }

    public String getIconColor() {
        return iconColor;
    }

    public void addMissing(SCStyle style) {
        if (this.fillColor != null) {
            this.fillColor = style.getFillColor();
        }
        if (this.fillOpacity != 0) {
            this.fillOpacity = style.getFillOpacity();
        }
        if (this.strokeColor != null) {
            this.strokeColor = style.getStrokeColor();
        }
        if (this.strokeOpacity != 0) {
            this.strokeOpacity = style.getStrokeOpacity();
        }
        if (this.iconColor != null) {
            this.iconColor = style.getIconColor();
        }
    }

    public void overwriteWith(SCStyle style) {
        if (style.getFillColor() != null) {
            this.fillColor = style.getFillColor();
        }
        if (style.getFillOpacity() != 0) {
            this.fillOpacity = style.getFillOpacity();
        }
        if (style.getStrokeColor() != null) {
            this.strokeColor = style.getStrokeColor();
        }
        if (style.getStrokeOpacity() != 0) {
            this.strokeOpacity = style.getStrokeOpacity();
        }
        if (style.getIconColor() != null) {
            this.iconColor = style.getIconColor();
        }
    }
}