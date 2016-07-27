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
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;
import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;

import rx.Observable;
import rx.Subscriber;

/**
 * Provides capabilities for interacting with a single GeoPackage.
 */
public class GeoPackageStore extends SCDataStore {

    private static final String LOG_TAG = GeoPackageStore.class.getSimpleName();
    public static final String TYPE = "gpkg";

    /**
     * Constructor for GeoPackageStore that initializes the data store adapter
     * based on the scStoreConfig.
     *
     * @param context       instance of the current activity's context
     * @param scStoreConfig instance of the configuration needed to configure the store
     */
    public GeoPackageStore(Context context, SCStoreConfig scStoreConfig) {
        super(context, scStoreConfig);
        this.setName(scStoreConfig.getName());
        this.setType(TYPE);
        this.setVersion(scStoreConfig.getVersion());
        this.getKey();
        // setup the adapter for this store
        GeoPackageAdapter adapter = new GeoPackageAdapter(context, scStoreConfig);
        this.setAdapter(adapter);
    }

    @Override
    public DataStorePermissionEnum getAuthorization() {
        return DataStorePermissionEnum.READ_WRITE;
    }


    @Override
    public Observable<SCSpatialFeature> query(final SCQueryFilter scFilter) {
        return ((GeoPackageAdapter) getAdapter()).query(scFilter);
    }

    @Override
    public Observable<SCSpatialFeature> queryById(final SCKeyTuple keyTuple) {
        return ((GeoPackageAdapter) getAdapter()).queryById(keyTuple);
    }

    @Override
    public Observable<SCSpatialFeature> create(final SCSpatialFeature scSpatialFeature) {
        return ((GeoPackageAdapter) getAdapter()).create(scSpatialFeature);
    }

    @Override
    public Observable<SCSpatialFeature> update(final SCSpatialFeature scSpatialFeature) {
        return ((GeoPackageAdapter) getAdapter()).update(scSpatialFeature);
    }

    @Override
    public Observable<Void> delete(final SCKeyTuple keyTuple) {
        return ((GeoPackageAdapter) getAdapter()).delete(keyTuple);
    }

    @Override
    public Observable<SCStoreStatusEvent> start() {
        final String storeId = this.getStoreId();
        final GeoPackageStore storeInstance = this;

        Log.d(LOG_TAG, "Starting store " + this.getName());
        storeInstance.setStatus(SCDataStoreStatus.SC_DATA_STORE_STARTED);

        return Observable.create(new Observable.OnSubscribe<SCStoreStatusEvent>() {
            @Override
            public void call(final Subscriber<? super SCStoreStatusEvent> subscriber) {

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
        });

    }

    @Override
    public void stop() {
        this.setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }
}
