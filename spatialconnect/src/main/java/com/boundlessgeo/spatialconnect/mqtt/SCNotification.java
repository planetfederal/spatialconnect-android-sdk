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
