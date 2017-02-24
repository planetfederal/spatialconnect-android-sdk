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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON object describing a SCStyle.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SCStyle {

    @JsonProperty("id")
    private String styleId;

    private Paint paint;

    public String getStyleId() {
        return styleId;
    }

    public void setStyleId(String styleId) {
        this.styleId = styleId;
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public void addMissing(SCStyle style) {
        if (this.paint != null && this.paint.getFillColor() != null) {
            this.paint.fillColor = style.getPaint().getFillColor();
        }
        if (this.paint != null && this.paint.getFillOpacity() != 0) {
            this.paint.fillOpacity = style.getPaint().getFillOpacity();
        }
        if (this.paint != null && this.paint.getStrokeColor() != null) {
            this.paint.strokeColor = style.getPaint().getStrokeColor();
        }
        if (this.paint != null && this.paint.getStrokeOpacity() != 0) {
            this.paint.strokeOpacity = style.getPaint().getStrokeOpacity();
        }
        if (this.paint != null && this.paint.getStrokeColor() != null) {
            this.paint.strokeColor = style.getPaint().getStrokeColor();
        }
//        if (this.strokeColor != null) {
//            this.strokeColor = style.getStrokeColor();
//        }
//        if (this.strokeOpacity != 0) {
//            this.strokeOpacity = style.getStrokeOpacity();
//        }
//        if (this.iconColor != null) {
//            this.iconColor = style.getIconColor();
//        }
    }
//
//    public void overwriteWith(SCStyle style) {
//        if (style.getFillColor() != null) {
//            this.fillColor = style.getFillColor();
//        }
//        if (style.getFillOpacity() != 0) {
//            this.fillOpacity = style.getFillOpacity();
//        }
//        if (style.getStrokeColor() != null) {
//            this.strokeColor = style.getStrokeColor();
//        }
//        if (style.getStrokeOpacity() != 0) {
//            this.strokeOpacity = style.getStrokeOpacity();
//        }
//        if (style.getIconColor() != null) {
//            this.iconColor = style.getIconColor();
//        }
//    }

    public static class Paint {
        @JsonProperty("fill_color")
        private String fillColor = null;

        @JsonProperty("fill_opacity")
        private float fillOpacity = 0;

        @JsonProperty("line_color")
        private String strokeColor = null;

        @JsonProperty("line_opacity")
        private float strokeOpacity = 0;

        @JsonProperty("icon_color")
        private String iconColor = null;

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
    }
}
