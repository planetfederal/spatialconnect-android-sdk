/**
 * Copyright 2016 Boundless, http://boundlessgeo.com
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

import com.boundlessgeo.spatialconnect.services.SCAuthService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
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
                .addNetworkInterceptor(new LoggingInterceptor())
                .addNetworkInterceptor(new SCAuthService.AuthHeaderInterceptor())
                .authenticator(new SCAuthService.SCAuthenticator())
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

    public Observable<Float> getWithProgress(final String url, final File file) throws IOException {
        return Observable.create(new Observable.OnSubscribe<Float>() {
            @Override
            public void call(Subscriber<? super Float> subscriber) {
                int DOWNLOAD_CHUNK_SIZE = 2028; //Same as Okio Segment.SIZE

                try {
                    Request request = new Request.Builder().url(url).build();

                    Response response = client.newCall(request).execute();
                    ResponseBody body = response.body();
                    long contentLength = body.contentLength();
                    BufferedSource source = body.source();
                    BufferedSink sink = Okio.buffer(Okio.sink(file));

                    long totalRead = 0;
                    long totalSinceLastPublish = 0;
                    long read;
                    while ((read = (source.read(sink.buffer(), DOWNLOAD_CHUNK_SIZE))) != -1) {
                        totalRead += read;
                        totalSinceLastPublish += read;
                        long progress = ((totalRead * 100) / contentLength);
                        if (totalSinceLastPublish > (contentLength / 16)) {
                            totalSinceLastPublish = 0;
                            subscriber.onNext((float) progress / 100);
                        }
                    }
                    sink.writeAll(source);
                    sink.flush();
                    sink.close();
                    subscriber.onNext(1f);
                    subscriber.onCompleted();
                } catch (IOException e) {
                    Log.e(LOG_TAG,"progress: Error: "+ e.getMessage());
                    subscriber.onError(e);
                }
            }
        })
        .subscribeOn(Schedulers.io());
    }

    public InputStream getResponseAsInputStream(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().byteStream();
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

    public Response getResponse(String theUrl) throws IOException {
        Request request = new Request.Builder()
                .url(theUrl)
                .build();
        return client.newCall(request).execute();
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
