/*
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

import com.boundlessgeo.schema.MessagePbf;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class SCNotification {

    private String to;
    private String title;
    private String body;
    private JSONObject payload;
    private String priority; // info, alert, background

    public SCNotification(MessagePbf.Msg message) {
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

    public HashMap<String, Object> toJSON() {
        HashMap<String, Object> json = new HashMap<>();

        json.put("to" , to);
        json.put("title", title);
        json.put("body", body);
        json.put("payload", payload);
        json.put("priority", priority);

        return json;
    }

}
