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

package com.boundlessgeo.spatialconnect.dataAdapter;


import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;

import java.net.MalformedURLException;
import java.net.URL;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

/**
 * This adpater connects to a specific geopackage defined in the scStoreConfig and provides an
 * instance of GeoPackageManager used to interact with the GeoPackages.
 */
public class GeoPackageAdapter extends SCDataAdapter {

    private static final String NAME = "GeoPackageAdapter";
    private static final String TYPE = "gpkg";
    private static final int VERSION = 1;
    private Context context;
    private final String LOG_TAG = GeoPackageAdapter.class.getSimpleName();
    private GeoPackageStore store;

    /**
     * GeoPackage manager
     */
    private GeoPackageManager manager;

    public GeoPackageAdapter(Context context, SCStoreConfig scStoreConfig, GeoPackageStore store) {
        super(NAME, TYPE, VERSION);
        this.context = context;
        this.scStoreConfig = scStoreConfig;
        // initialize geopackage manager
        manager = GeoPackageFactory.getManager(context);
        this.store = store;
    }

    @Override
    public Observable<SCDataAdapterStatus> connect() {
        final GeoPackageAdapter adapterInstance = this;

        return Observable.create(new Observable.OnSubscribe<SCDataAdapterStatus>() {
             @Override
             public void call(final Subscriber<? super SCDataAdapterStatus> subscriber) {

                 adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTING);

                 URL theUrl = null;
                 try {
                     // TODO: we need to determine if the store's URI is local to the filesystem (packaged
                     // with the app or on an SD card and try loading from there)
                     theUrl = new URL(scStoreConfig.getUri());
                 } catch (MalformedURLException e) {
                     Log.e(LOG_TAG, "URL was malformed. Check the syntax: " + theUrl);
                     adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
                     subscriber.onError(e);
                 }

                 // download geopackage and store it locally if it's not present
                 if (!manager.exists(scStoreConfig.getName())) {
                     final URL url = theUrl;
                     Log.d(LOG_TAG, "Attempting to download geopackage from " + url);
                     store.setStatus(SCDataStoreStatus.SC_DATA_STORE_DOWNLOADING);

                     downloadGeopackage(scStoreConfig, theUrl)
                             .subscribeOn(Schedulers.newThread())
                             .observeOn(AndroidSchedulers.mainThread())
                             .subscribe(new Action1<Boolean>() {
                               @Override
                               public void call(Boolean downloaded) {

                                 if (!downloaded) {
                                   Log.w(LOG_TAG, "Failed to import GeoPackage from " + url);
                                   Log.w(LOG_TAG, "Could not connect to GeoPackage");
                                   adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
                                   store.setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
                                   subscriber.onError(new Throwable("Failed to import GeoPackage from " + url));

                                 } else {
                                   Log.d(LOG_TAG, "Successfully downloaded geopackage from " + url);
                                   // connect to the geopackage on the local filesystem
                                   GeoPackage geoPackage = manager.open(scStoreConfig.getName());

                                   if (geoPackage != null) {
                                     adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED);
                                     store.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                                     subscriber.onCompleted();
                                   } else {
                                     adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
                                     store.setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
                                     subscriber.onError(new Throwable("geopackage was null"));
                                   }
                                 }
                               }
                             }, new Action1<Throwable>() {
                               @Override
                               public void call(Throwable throwable) {
                                 //Error if Geopackage download can't be reached
                                 subscriber.onError(throwable);
                               }
                             });
                 } else {
                     Log.d(LOG_TAG, "GeoPackage " + scStoreConfig.getName() + " already downloaded.");
                     adapterInstance.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED);
                     store.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                     subscriber.onCompleted();
                 }
             }
         });
    }

    public GeoPackageManager getGeoPackageManager() {
        return manager;
    }

    // neat trick learned from http://blog.danlew.net/2014/10/08/grokking-rxjava-part-4/#oldslowcode
    public Observable<Boolean> downloadGeopackage(final SCStoreConfig scStoreConfig, final URL theUrl) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                return Observable.just(manager.importGeoPackage(scStoreConfig.getName(), theUrl));
            }
        });
    }


}
