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
package com.boundlessgeo.spatialconnect.mqtt;

import android.util.Log;

import com.boundlessgeo.spatialconnect.schema.SCMessageOuterClass;

import org.json.JSONException;
import org.json.JSONObject;

public class SCNotification {

    private String to;
    private String title;
    private String body;
    private JSONObject payload;
    private String priority; // info, alert, background

    public SCNotification(SCMessageOuterClass.SCMessage message) {
        try {
            JSONObject payload = new JSONObject(message.getPayload());
            this.to = payload.getString("to");
            this.title = payload.getString("title");
            this.body = payload.getString("body");
            this.payload = payload.getJSONObject("payload");
            this.priority = payload.getString("priority");
        }
        catch (JSONException e) {
            Log.e("SCNotification", "Could not parse message payload into json", e);
        }
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("to" , to);
            obj.put("title", title);
            obj.put("body", body);
            obj.put("payload", payload);
            obj.put("priority", priority);
        }
        catch (JSONException e) {
            Log.e("SCNotification", "Could not transform notification into json", e);
        }
        return obj;
    }

}
