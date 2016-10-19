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
package com.boundlessgeo.spatialconnect.dataAdapter;


import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryCollection;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryFactory;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;


public class GeoJsonAdapter extends SCDataAdapter {
    private static final String NAME = "GeoJsonAdapter";
    private static final String TYPE = "GeoJson";
    private static final int VERSION = 1;
    public static final String DEFAULTLAYER = "default";
    private Context context;
    private static final String LOG_TAG = GeoJsonAdapter.class.getSimpleName();
    private final String EXT = ".json";
    private String geojsonFilePath;

    public GeoJsonAdapter(Context context, SCStoreConfig scStoreConfig) {
        super(NAME, TYPE, VERSION);
        this.context = context;
        this.scStoreConfig = scStoreConfig;
    }

    public Observable connect() {
        final GeoJsonAdapter adapterInstance = this;
        Log.d(LOG_TAG, "Attempting to connect GeoJson store at " + scStoreConfig.getUri());

        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber subscriber) {
                adapterInstance.connect();

                final String filePath = scStoreConfig.getUniqueID() + EXT;
                final File geoJsonFile = new File(context.getFilesDir(), filePath); //new File(filePath);

                if (!geoJsonFile.exists()) {
                    if (scStoreConfig.getUri().startsWith("http")) {
                        //download from web
                        try {
                            URL theUrl = new URL(scStoreConfig.getUri());
                            HttpHandler.getInstance().get(theUrl.toString())
                                    .subscribe(new Action1<Response>() {
                                        @Override
                                        public void call(Response response) {
                                            if (!response.isSuccessful()) {
                                                adapterInstance.disconnect();
                                            }
                                            try {
                                                // save response as file
                                                FileUtils.copyInputStreamToFile(response.body().byteStream(), geoJsonFile);
                                                geojsonFilePath = filePath;
                                                adapterInstance.connected();
                                                subscriber.onCompleted();
                                            } catch (IOException e) {
                                                Log.w(LOG_TAG, "Couldn't download geojson store.", e);
                                                adapterInstance.disconnect();
                                                e.printStackTrace();
                                                subscriber.onError(e);
                                            } finally {
                                                response.body().close();
                                            }
                                        }
                                    });
                        } catch (MalformedURLException e) {
                            Log.e(LOG_TAG, "URL was malformed. Check the syntax: " + scStoreConfig.getUri());
                            adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
                            subscriber.onError(e);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //attempt to find file local
                        final String localUriPath = scStoreConfig.getUri().replace("file://", "");
                        File f = new File(context.getFilesDir(), localUriPath);
                        if (!f.exists()) {
                            Log.d(LOG_TAG, "File does not exist at " + scStoreConfig.getUri());
                            InputStream is = null;
                            // if the doesn't exist, then we attempt to create it by copying it from the raw
                            // resources to the destination specified in the config
                            try {
                                Log.d(LOG_TAG, "Attempting to connect to " + scStoreConfig.getUri().split("\\.")[0]);
                                int resourceId = context.getResources().getIdentifier(
                                        scStoreConfig.getUri().split("\\.")[0], "raw", context.getPackageName()
                                );
                                if (resourceId != 0) {
                                    is = context.getResources().openRawResource(resourceId);
                                    FileUtils.copyInputStreamToFile(is, f);
                                    geojsonFilePath = localUriPath;
                                    adapterInstance.connected();
                                    subscriber.onCompleted();
                                } else {
                                    String errorString = "The config specified a store that should exist on the " +
                                            "filesystem but it could not be located.";
                                    Log.w(LOG_TAG, errorString);
                                    subscriber.onError(new Throwable(errorString));
                                }
                            } catch (Exception e) {
                                Log.w(LOG_TAG, "Couldn't connect to geojson store.", e);
                                adapterInstance.disconnect();
                                e.printStackTrace();
                                subscriber.onError(e);
                            } finally {
                                if (is != null) {
                                    try {
                                        is.close();
                                    } catch (IOException e) {
                                        Log.e(LOG_TAG, "Couldn't close the stream.", e);
                                    }
                                }
                            }
                        } else {
                            Log.d(LOG_TAG, "File already exists so set the status to connected.");
                            geojsonFilePath = localUriPath;
                            adapterInstance.connected();
                            subscriber.onCompleted();
                        }
                    }

                } else {
                    geojsonFilePath = filePath;
                    adapterInstance.connected();
                    subscriber.onCompleted();
                }
            }
        });
    }

    public void disconnect() {
        super.disconnect();
    }

    // using a GeoJSON file to queryFeature is not going to be fast
    public Observable<SCSpatialFeature> query(final SCQueryFilter filter) {
        SCGeometryFactory factory = new SCGeometryFactory();
        // TODO: updated the Json --> SCSpatialFetures code to use InputStreams instead of Strings
        final SCGeometryCollection collection = factory.getGeometryCollectionFromFeatureCollectionJson(
                getResourceAsString()
        );

        return Observable.from(collection.getFeatures())
                .filter(
                        new Func1<SCSpatialFeature, Boolean>() {
                            @Override
                            public Boolean call(SCSpatialFeature feature) {
                                if (feature instanceof SCGeometry &&
                                        ((SCGeometry) feature).getGeometry() != null &&
                                        filter.getPredicate().applyFilter((SCGeometry) feature)) {
                                    return true;
                                }
                                else {
                                    return false;
                                }
              }
            }
          ).map(new Func1<SCSpatialFeature, SCSpatialFeature>() {
            @Override
            public SCSpatialFeature call(SCSpatialFeature scSpatialFeature) {
              scSpatialFeature.setStoreId(scStoreConfig.getUniqueID());
              scSpatialFeature.setLayerId(DEFAULTLAYER);
              scSpatialFeature.setId(scSpatialFeature.getId());
              return scSpatialFeature;
            }
          });

    }


    public void create(SCSpatialFeature spatialFeatureToAdd) {

    }

    public void update(SCSpatialFeature scSpatialFeature) {

    }

    public void delete(String featureId) {

    }

    public List<String> layers() {
        return Arrays.asList(DEFAULTLAYER);
    }


    private InputStream getResourceStream() {
        int resourceId = context.getResources().getIdentifier(
                scStoreConfig.getUri().split("\\.")[0], "raw", context.getPackageName()
        );
        return context.getResources().openRawResource(resourceId);
    }

    /**
     * Returns a String of the GeoJson file associated with the adapter's data store.
     *
     * @return
     */
    private String getResourceAsString() {
        InputStream is = null;
        StringBuilder stringBuilder = null;
        try {
            final String filePath = scStoreConfig.getUniqueID() + EXT;
            final File geoJsonFile = new File(context.getFilesDir(), geojsonFilePath);
            Log.e(LOG_TAG, "geojsonfilepath " + geojsonFilePath);
            is = FileUtils.openInputStream(geoJsonFile);

            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(is));
            String line;
            stringBuilder = new StringBuilder();

            while ((line = bufferedreader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Couldn't read the stream.", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Couldn't close the stream.", e);
            }
        }
        return stringBuilder.toString();
    }

}



