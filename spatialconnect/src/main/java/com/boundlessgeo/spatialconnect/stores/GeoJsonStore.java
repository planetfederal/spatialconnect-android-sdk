/**
 * Copyright 2015-2016 Boundless, http://boundlessgeo.com
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

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoJsonAdapter;
import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;

import java.util.List;

import rx.Observable;
import rx.Subscriber;

public class GeoJsonStore extends SCDataStore implements SCSpatialStore, SCDataStoreLifeCycle {

    private static final String LOG_TAG = GeoJsonStore.class.getSimpleName();
    public static final String TYPE = "geojson";

    public GeoJsonStore(Context context, SCStoreConfig scStoreConfig) {
        super(context, scStoreConfig);
        this.setName(scStoreConfig.getName());
        this.setType(TYPE);
        this.setVersion(scStoreConfig.getVersion());
        this.setAdapter(new GeoJsonAdapter(context, scStoreConfig));
    }

    public List<String> defaultLayers() {
        return null;
    }

    public List<String> layers() {
        return this.vectorLayers();
    }

    public List<String> vectorLayers() {
        GeoJsonAdapter geoJsonAdapter = (GeoJsonAdapter) this.getAdapter();
        return geoJsonAdapter.layers();
    }

    @Override
    public Observable<SCSpatialFeature> query(SCQueryFilter scFilter) {
        GeoJsonAdapter geoJsonAdapter = (GeoJsonAdapter) this.getAdapter();
        return geoJsonAdapter.query(scFilter);
    }

    @Override
    public Observable<SCSpatialFeature> queryById(SCKeyTuple keyTuple) {
        return query(null);
    }

    @Override
    public Observable<Boolean> create(final SCSpatialFeature scSpatialFeature) {
        GeoJsonAdapter geoJsonAdapter = (GeoJsonAdapter) this.getAdapter();
        geoJsonAdapter.create(scSpatialFeature);
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
        GeoJsonAdapter geoJsonAdapter = (GeoJsonAdapter) this.getAdapter();
        geoJsonAdapter.update(scSpatialFeature);
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
        GeoJsonAdapter geoJsonAdapter = (GeoJsonAdapter) this.getAdapter();
        geoJsonAdapter.delete(keyTuple.getFeatureId());
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

    public Observable start() {
        final String storeId = this.getStoreId();
        final GeoJsonStore storeInstance = this;
        storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_STARTED);
        return Observable.create(
                new Observable.OnSubscribe<SCStoreStatusEvent>() {
                    @Override
                    public void call(final Subscriber<? super SCStoreStatusEvent> subscriber) {
                        Log.d(LOG_TAG, "Connecting to GeoJson adapter for store " + storeInstance.getName());

                        // subscribe to an Observable/stream that lets us know when the adapter is connected or disconnected
                        storeInstance.getAdapter().connect().subscribe(new Subscriber<SCDataAdapterStatus>() {

                            @Override
                            public void onCompleted() {
                                storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.w(LOG_TAG, "Could not activate data store " + storeId);
                                storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
                                subscriber.onError(e);
                            }

                            @Override
                            public void onNext(SCDataAdapterStatus status) {
                            }
                        });
                    }
                }
        );

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
}
