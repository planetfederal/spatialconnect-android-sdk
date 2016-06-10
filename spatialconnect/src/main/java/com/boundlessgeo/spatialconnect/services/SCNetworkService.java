/**
 * Copyright 2015-2016 Boundless, http://boundlessgeo.com
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
package com.boundlessgeo.spatialconnect.services;


import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class SCNetworkService extends SCService {

    private static final String LOG_TAG = SCNetworkService.class.getSimpleName();
    private static Context context;
    private static OkHttpClient client = new OkHttpClient();
    // TODO: this should be set as an environment variable or as part of the build
    public static final String API_URL = "http://efc.boundlessgeo.com:8085/";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    // TODO: we should persist this in the app storage so it doesn't change every time we run the app
    public String clientId = UUID.randomUUID().toString();

    public SCNetworkService(Context context) {
        this.context = context;
        registerDevice();
    }

    private void registerDevice() {
        String android_id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        Log.d(LOG_TAG, "Registering the device with name " + "android_" + android_id + " and id " + clientId);
        try {
            post(API_URL + "device/register",
                    String.format("{\"name\": \"%s\", \"identifier\": \"%s\"}", "android_" + android_id, clientId)
            );
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Couldn't register device");
            System.exit(0);
        }
    }

    public File getFileBlocking(final String theUrl) {
        Log.d(LOG_TAG, "Fetching remote config from " + theUrl);
        Request request = new Request.Builder()
                .url(theUrl)
                .build();

        Response response;
        File scConfigFile = null;
        try {
            response = client.newCall(request).execute();
            BufferedInputStream is = new BufferedInputStream(response.body().byteStream());
            scConfigFile = File.createTempFile(UUID.randomUUID().toString(), null, context.getCacheDir());
            OutputStream os = new FileOutputStream(scConfigFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            os.close();
            is.close();
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Could not download file");
            e.printStackTrace();
        }
        return scConfigFile;
    }

    public static OkHttpClient getHttpClient() {
        return client;
    }

    public String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public String post(String url, String json) throws IOException {
        String jsonBody = json.substring(0, json.length()-1) + ",\"metadata\": {\"client\":\"" + clientId + "\"}}";
        Log.d(LOG_TAG, "POST " + url + "\n" + jsonBody);
        RequestBody body = RequestBody.create(JSON, jsonBody);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

}
