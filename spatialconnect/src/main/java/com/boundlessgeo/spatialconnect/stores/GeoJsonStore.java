/*
 *
 *  * ****************************************************************************
 *  *  Licensed to the Apache Software Foundation (ASF) under one
 *  *  or more contributor license agreements.  See the NOTICE file
 *  *  distributed with this work for additional information
 *  *  regarding copyright ownership.  The ASF licenses this file
 *  *  to you under the Apache License, Version 2.0 (the
 *  *  "License"); you may not use this file except in compliance
 *  *  with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing,
 *  *  software distributed under the License is distributed on an
 *  *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  *  KIND, either express or implied.  See the License for the
 *  *  specific language governing permissions and limitations
 *  *  under the License.
 *  * ****************************************************************************
 *
 */

package com.boundlessgeo.spatialconnect.stores;


import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoJsonAdapter;
import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;

import rx.Observable;
import rx.Subscriber;

public class GeoJsonStore extends SCDataStore {
    private static final String LOG_TAG = GeoJsonStore.class.getSimpleName();
    private static String TYPE = "geojson";
    private static int VERSION = 1;

    public static String versionKey() {
        return TYPE + "." + VERSION;
    }

    public GeoJsonStore(Context context, SCStoreConfig scStoreConfig) {
        super(context, scStoreConfig);
        this.setName(scStoreConfig.getName());
        this.setType(TYPE);
        this.setVersion(VERSION);
        this.setAdapter(new GeoJsonAdapter(context, scStoreConfig));
    }


    @Override
    public Observable<SCSpatialFeature> query(SCQueryFilter scFilter) {
        GeoJsonAdapter geoJsonAdapter = (GeoJsonAdapter) this.getAdapter();
        Observable<SCSpatialFeature> queryResults = geoJsonAdapter.query(scFilter);
        return queryResults;
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
                } catch (Exception e) {
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
                } catch (Exception e) {
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
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Could not create delete with id " + keyTuple.getFeatureId());
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        });
    }

    public Observable start() {
        this.setStatus(SCDataStoreStatus.SC_DATA_STORE_STARTED);
        this.getAdapter().connect();
        if (this.getAdapter().getStatus().equals(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED)) {
            this.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
            return Observable.empty();
        } else {
            stop();
            return Observable.error(new Exception("Error starting GeoJSON store"));
        }
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
