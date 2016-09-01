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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
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
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;


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

    public Response getResponse(String theUrl) throws IOException {
        Request request = new Request.Builder()
                .url(theUrl)
                .build();
        return client.newCall(request).execute();
    }

    public void cancelAllRequests() {
        client.dispatcher().cancelAll();
    }

    // from https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/Progress.java
    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        public ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }

    public boolean isInternetAvailable() {

        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = cm.getAllNetworks();
            NetworkInfo networkInfo;

            for (Network ni : networks) {
                networkInfo = cm.getNetworkInfo(ni);
                if (networkInfo.getTypeName().equalsIgnoreCase("WIFI"))
                    if (networkInfo.isConnected())
                        haveConnectedWifi = true;
                if (networkInfo.getTypeName().equalsIgnoreCase("MOBILE"))
                    if (networkInfo.isConnected())
                        haveConnectedMobile = true;
            }
        } else {
            NetworkInfo[] netInfo = cm.getAllNetworkInfo();
            for (NetworkInfo ni : netInfo) {
                if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                    if (ni.isConnected())
                        haveConnectedWifi = true;
                if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                    if (ni.isConnected())
                        haveConnectedMobile = true;
            }
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    interface ProgressListener {
        void update(long bytesRead, long contentLength, boolean done);
    }

    final ProgressListener progressListener = new ProgressListener() {
        @Override
        public void update(long bytesRead, long contentLength, boolean done) {
            // TODO: consider wrapping the  update in an observable that you can throttle
            // something like this: https://groups.google.com/forum/#!msg/rxjava/aqDM7Eq3zT8/PCF_7pdlGgAJ
//                Log.v(LOG_TAG, String.format("%d%% done\n", (100 * bytesRead) / contentLength));
        }
    };
}
