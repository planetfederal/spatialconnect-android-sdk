package com.boundlessgeo.spaconapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class MainActivity extends AppCompatActivity {

    private static SpatialConnect sc;
    protected static File remoteConfigFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        try {
//
//            remoteConfigFile = File.createTempFile("config_remote.scfg", null, getCacheDir());
//
//            // read test scconfig_remote.json file from test resources directory
//            InputStream is = getResources().openRawResource(R.raw.scconfig_remote);
//            FileOutputStream fos = new FileOutputStream(remoteConfigFile);
//            byte[] data = new byte[is.available()];
//            is.read(data);
//            fos.write(data);
//
//            is.close();
//            fos.close();
////http://efc-dev.boundlessgeo.com:8080/geoserver/spatialconnect/ows?service=WFS&version=1.1.0&request=GetCapabilities
////https://s3.amazonaws.com/test.spacon/rio.gpkg
//
//            sc = SpatialConnect.getInstance();
//            sc.initialize(this);
//            sc.addConfig(remoteConfigFile);
//            sc.getAuthService().start();
//            //sc.startAllServices();
//            sc.getAuthService().authenticate("admin@something.com", "admin");
////            HttpHandler.getInstance().getWithProgress("https://s3.amazonaws.com/test.spacon/rio.gpkg")
////                    .subscribe(new Action1<Response>() {
////                        @Override
////                        public void call(Response response) {
////                            Log.e("MainActin","Done>>>");
////                        }
////                    });
//
//            Observable.create(new Observable.OnSubscribe<String>() {
//                @Override
//                public void call(final Subscriber<? super String> subscriber) {
//                    // try to connect to WFS store to get the layers from the capabilities documents
//                    try {
//                        HttpHandler.getInstance().get("http://efc-dev.boundlessgeo.com:8080/geoserver/spatialconnect/ows?service=WFS&version=1.1.0&request=GetCapabilities")
//                                .subscribe(new Action1<Response>() {
//                                    @Override
//                                    public void call(Response response) {
//                                        subscriber.onNext("done");
//                                        subscriber.onCompleted();
//                                    }
//                                });
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//        } catch (java.io.IOException e) {
//            e.printStackTrace();
//        }

        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        runs();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runs() throws  Exception {
//        Request request = new Request.Builder()
//                .url("https://publicobject.com/helloworld.txt")
//                .build();
//
//        final ProgressListener progressListener = new ProgressListener() {
//            @Override public void update(long bytesRead, long contentLength, boolean done) {
//                System.out.println(bytesRead);
//                System.out.println(contentLength);
//                System.out.println(done);
//                System.out.format("%d%% done\n", (100 * bytesRead) / contentLength);
//            }
//        };
//
        OkHttpClient client = new OkHttpClient.Builder()
//                .addNetworkInterceptor(new Interceptor() {
//                    @Override public Response intercept(Chain chain) throws IOException {
//                        Response originalResponse = chain.proceed(chain.request());
//                        return originalResponse.newBuilder()
//                                .body(new ProgressResponseBody(originalResponse.body(), progressListener))
//                                .build();
//                    }
//                })
                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
//            System.out.println(response.body().string());
//        }

        int DOWNLOAD_CHUNK_SIZE = 2048; //Same as Okio Segment.SIZE

        try {
            Request request = new Request.Builder().url("https://s3.amazonaws.com/test.spacon/rio.gpkg").build();

            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            long contentLength = body.contentLength();
            BufferedSource source = body.source();

            File file = new File(getFilesDir(), "android.gpkg");
            BufferedSink sink = Okio.buffer(Okio.sink(file));

            long totalRead = 0;
            long totalSinceLastPublish = 0;
            long read;
            Log.e("MainActivity","contentLength: " + contentLength / 4);
            while ((read = (source.read(sink.buffer(), DOWNLOAD_CHUNK_SIZE))) != -1) {
                totalRead += read;
                totalSinceLastPublish += read;
                int progress = (int) ((totalRead * 100) / contentLength);
                //Log.e("MainActivity", "totalSinceLastPublish: " + totalSinceLastPublish);
                if (totalSinceLastPublish > (contentLength / 4)) {
                    totalSinceLastPublish = 0;
                    Log.e("MainActivity", "progress: " + progress);
                }
//                publishProgress(progress);
            }
            sink.writeAll(source);
            sink.flush();
            sink.close();
//            publishProgress(FileInfo.FULL);
            Log.e("MainActivity","progress: DONE");
        } catch (IOException e) {
            Log.e("MainActivity","progress: Error: "+ e.getMessage());
//            publishProgress(FileInfo.CODE_DOWNLOAD_ERROR);
//            Logger.reportException(e);
        }
    }

    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        public ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override public long contentLength() {
            return responseBody.contentLength();
        }

        @Override public BufferedSource source() {
            Log.e("MainActivity","source");
            if (bufferedSource == null) {
                Log.e("MainActivity","source-0");
                bufferedSource = Okio.buffer(source(responseBody.source()));
            } else {
                Log.e("MainActivity","source-1");
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    Log.e("MainActivity","read");
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
}
