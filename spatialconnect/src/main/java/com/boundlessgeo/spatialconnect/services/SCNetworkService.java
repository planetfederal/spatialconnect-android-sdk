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
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


// TODO: consider using https://github.com/stephanenicolas/robospice as this class evolves
public class SCNetworkService extends SCService {

    private static final String LOG_TAG = SCNetworkService.class.getSimpleName();
    private static Context context;
    private static OkHttpClient client;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public SCNetworkService(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new LoggingInterceptor())
                .addNetworkInterceptor(new SCAuthService.AuthHeaderInterceptor())
                .authenticator(new SCAuthService.SCAuthenticator())
                .build();
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
            if (response.isSuccessful()) {
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
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Could not download file");
            e.printStackTrace();
        }
        return scConfigFile;
    }

    public String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public InputStream getResponseAsInputStream(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().byteStream();
    }

    public String post(String url, String json) throws IOException {
        Log.d(LOG_TAG, "POST " + url + "\n" + json);
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request request = chain.request();

            long t1 = System.nanoTime();
            Log.d(LOG_TAG, String.format("Sending request %s on %s%n%s",
                    request.url(), chain.connection(), request.headers()));

            Response response = chain.proceed(request);

            long t2 = System.nanoTime();
            Log.d(LOG_TAG, String.format("Received %d response for %s in %.1fms%n%s",
                    response.code(), response.request().url(), (t2 - t1) / 1e6d, response.headers()));

            return response;
        }
    }

}
