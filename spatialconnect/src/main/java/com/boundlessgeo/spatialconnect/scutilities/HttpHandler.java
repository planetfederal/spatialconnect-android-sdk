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
package com.boundlessgeo.spatialconnect.scutilities;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class HttpHandler {

    private final static String LOG_TAG = HttpHandler.class.getSimpleName();

    private static HttpHandler instance;
    private static OkHttpClient client;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public static HttpHandler getInstance() {
        if (instance == null) {
            instance = new HttpHandler();
        }
        return instance;
    }

    private HttpHandler() {
        this.client = new OkHttpClient.Builder()
                .readTimeout(2, TimeUnit.MINUTES)
                .addNetworkInterceptor(new LoggingInterceptor())
                .build();
    }

    public Observable<Response> get(final String url) throws IOException {
        return Observable.create(new Observable.OnSubscribe<Response>() {
            @Override
            public void call(Subscriber<? super Response> subscriber) {
                try {
                    Request request = new Request.Builder()
                            .url(url)
                            .build();
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        subscriber.onError(new Exception("error"));
                    } else {
                        subscriber.onNext(response);
                        subscriber.onCompleted();
                    }
                } catch (IOException e) {
                    subscriber.onError(e);
                }
            }
        })
        .subscribeOn(Schedulers.io());
    }

    public Observable<SCTuple<Float, byte[], Integer>> getWithProgress(final String url) {
        return Observable.create(new Observable.OnSubscribe<SCTuple<Float, byte[], Integer>>() {
            @Override
            public void call(Subscriber<? super SCTuple<Float, byte[], Integer>> subscriber) {
                try {
                    final URL downloadFileUrl = new URL(url);
                    final HttpURLConnection connection = (HttpURLConnection) downloadFileUrl.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setDoInput(true);
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.connect();

                    int contentLength = connection.getContentLength();
                    final byte buffer[] = new byte[2048];
                    final InputStream inputStream = connection.getInputStream();

                    long total = 0;
                    int count;
                    float progress;

                    while ((count = inputStream.read(buffer)) != -1) {
                        total += count;
                        progress = (total / (float)contentLength);
                        subscriber.onNext((SCTuple<Float, byte[], Integer>)new SCTuple(progress, buffer, count));
                    }
                    subscriber.onNext((SCTuple<Float, byte[], Integer>)new SCTuple(1f, buffer, count));
                    subscriber.onCompleted();

                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public Observable<Response> post(final String url, final String json) {
        return Observable.create(new Observable.OnSubscribe<Response>() {
            @Override
            public void call(Subscriber<? super Response> subscriber) {
                try {
                    RequestBody body = RequestBody.create(JSON, json);
                    Request request = new Request.Builder()
                            .url(url)
                            .post(body)
                            .build();
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        subscriber.onError(new Exception("error"));
                    } else {
                        subscriber.onNext(response);
                        subscriber.onCompleted();
                    }
                } catch (IOException e) {
                    subscriber.onError(e);
                }
            }
        })
        .subscribeOn(Schedulers.io());
    }

    public void cancelAllRequests() {
        client.dispatcher().cancelAll();
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
