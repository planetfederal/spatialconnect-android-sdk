/**
 * Copyright 2015-2017 Boundless, http://boundlessgeo.com
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.stores;


import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryCollection;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryFactory;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

public class GeoJsonStore extends SCDataStore implements ISCSpatialStore, SCDataStoreLifeCycle {

    private static final String LOG_TAG = GeoJsonStore.class.getSimpleName();
    public static final String TYPE = "geojson";
    protected SCStoreConfig scStoreConfig;
    private static final int VERSION = 1;
    public static final String DEFAULTLAYER = "default";
    private Context context;
    private final String EXT = ".json";
    private String geojsonFilePath;

    public GeoJsonStore(Context context, SCStoreConfig scStoreConfig) {
        super(context, scStoreConfig);
        this.context = context;
        this.setName(scStoreConfig.getName());
        this.setType(TYPE);
        this.setVersion(scStoreConfig.getVersion());
        this.scStoreConfig = scStoreConfig;
    }

    public List<String> layers() {
        return this.vectorLayers();
    }

    public List<String> vectorLayers() {
        return Arrays.asList(DEFAULTLAYER);
    }

    @Override
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

    @Override
    public Observable<SCSpatialFeature> queryById(SCKeyTuple keyTuple) {
        return query(null);
    }

    @Override
    public Observable<Boolean> create(final SCSpatialFeature scSpatialFeature) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    // TODO: implement functionality here
                    subscriber.onNext(true);
                }
                catch (Exception e) {
                    Log.e(LOG_TAG, "Could not create feature with id " + scSpatialFeature.getId());
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<Boolean> update(final SCSpatialFeature scSpatialFeature) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    // TODO: implement functionality here
                    subscriber.onNext(true);
                }
                catch (Exception e) {
                    Log.e(LOG_TAG, "Could not update feature with id " + scSpatialFeature.getId());
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<Boolean> delete(final SCKeyTuple keyTuple) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    // TODO: implement functionality here
                    subscriber.onNext(true);
                }
                catch (Exception e) {
                    Log.e(LOG_TAG, "Could not create delete with id " + keyTuple.getFeatureId());
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        });
    }

    public Observable<SCStoreStatusEvent> start() {
        final GeoJsonStore storeInstance = this;
        storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_STARTED);

        return Observable.create(new Observable.OnSubscribe<SCStoreStatusEvent>() {
            @Override
            public void call(final Subscriber subscriber) {
                final String filePath = scStoreConfig.getUniqueID() + EXT;
                final File geoJsonFile = new File(getPath());

                if (!geoJsonFile.exists()) {
                    if (scStoreConfig.getUri().startsWith("http")) {
                        //download from web
                        try {
                            URL theUrl = new URL(scStoreConfig.getUri());
                            download(theUrl.toString(), geoJsonFile)
                                    .subscribe(
                                        new Action1<Float>() {
                                            @Override
                                            public void call(Float progress) {
                                               setDownloadProgress(progress);
                                                if (progress < 1) {
                                                    setStatus(SCDataStoreStatus.SC_DATA_STORE_DOWNLOADING_DATA);
                                                    subscriber.onNext(new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_DOWNLOADING_DATA));
                                                } else {
                                                    setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                                                    geojsonFilePath = filePath;
                                                    subscriber.onCompleted();
                                                }
                                            }
                                        },
                                        new Action1<Throwable>() {
                                            @Override
                                            public void call(Throwable t) {
                                                subscriber.onNext(new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_START_FAILED));
                                            }
                                        }
                                    );
                        } catch (IOException e) {
                            Log.w(LOG_TAG, "Couldn't download geojson store.", e);
                            GeoJsonStore parentStore =
                                    (GeoJsonStore) SpatialConnect.getInstance().getDataService().getStoreByIdentifier(scStoreConfig.getUniqueID());
                            parentStore.setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
                            e.printStackTrace();
                            subscriber.onError(e);
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
                            GeoJsonStore parentStore =
                                    (GeoJsonStore) SpatialConnect.getInstance().getDataService().getStoreByIdentifier(scStoreConfig.getUniqueID());
                            try {
                                Log.d(LOG_TAG, "Attempting to connect to " + scStoreConfig.getUri().split("\\.")[0]);
                                int resourceId = context.getResources().getIdentifier(
                                        scStoreConfig.getUri().split("\\.")[0], "raw", context.getPackageName()
                                );
                                if (resourceId != 0) {
                                    is = context.getResources().openRawResource(resourceId);
                                    FileUtils.copyInputStreamToFile(is, f);
                                    geojsonFilePath = localUriPath;
                                    parentStore.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                                    subscriber.onCompleted();
                                } else {
                                    String errorString = "The config specified a store that should exist on the " +
                                            "filesystem but it could not be located.";
                                    Log.w(LOG_TAG, errorString);
                                    parentStore.setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
                                    subscriber.onError(new Throwable(errorString));
                                }
                            } catch (Exception e) {
                                Log.w(LOG_TAG, "Couldn't connect to geojson store.", e);
                                parentStore.setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
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
                            GeoJsonStore parentStore =
                                    (GeoJsonStore) SpatialConnect.getInstance().getDataService().getStoreByIdentifier(scStoreConfig.getUniqueID());
                            parentStore.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                            //adapterInstance.connected();
                            subscriber.onCompleted();
                        }
                    }

                } else {
                    GeoJsonStore parentStore =
                            (GeoJsonStore) SpatialConnect.getInstance().getDataService().getStoreByIdentifier(scStoreConfig.getUniqueID());
                    parentStore.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                    geojsonFilePath = filePath;
                    subscriber.onCompleted();
                }
            }
        });

    }

    public void stop() {
        this.setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
    }

    public void pause() {
        this.setStatus(SCDataStoreStatus.SC_DATA_STORE_PAUSED);
    }

    public void resume() {
        this.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
    }

    public void destroy() {
        deleteFile(getPath());
    }

    public String getPath() {
        StringBuilder filePath = new StringBuilder();
        filePath.append(context.getFilesDir());
        filePath.append(File.separator);
        filePath.append(scStoreConfig.getUniqueID());
        filePath.append(EXT);
        return filePath.toString();
    }

    public static String getVersionKey() {
        return String.format("%s.%s",TYPE, VERSION);
    }
    private String getResourceAsString() {
        InputStream is = null;
        StringBuilder stringBuilder = null;
        try {
            final File geoJsonFile = new File(context.getFilesDir(), geojsonFilePath);
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
