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
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.db.SCStoreConfigRepository;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.stores.DefaultStore;
import com.boundlessgeo.spatialconnect.stores.FormStore;
import com.boundlessgeo.spatialconnect.stores.GeoJsonStore;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.LocationStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCStoreStatusEvent;
import com.boundlessgeo.spatialconnect.stores.WFSStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;

/**
 * The SCDataService is responsible for starting, stopping, and managing access to {@link SCDataStore} instances.  This
 * includes exposing methods to query across multiple SCDataStore instances.
 */
public class SCDataService extends SCService {

    private static final String LOG_TAG = SCDataService.class.getSimpleName();
    private Set<String> supportedStores; // the strings are store keys: type.version
    private Map<String, SCDataStore> stores;
    private Context context;

    /**
     * The storeEventSubject is like an internal event bus, its job is to receive {@link SCStoreStatusEvent}s
     * published by a {@link SCDataStore}.
     */
    protected BehaviorSubject<SCStoreStatusEvent> storeEventSubject;

    /**
     * This is the Observable that subscribers in your app will subscribe to so that they can receive {@link
     * SCStoreStatusEvent}s.  Subscribers must explicitly call {@link rx.observables.ConnectableObservable#connect()}
     * connect to start receiving events b/c storeEvents is a ConnectableObservable. We use a ConnectableObservable
     * so that multiple subscribers can connect before beginning to emit the events about the {@link SCDataStore}s.
     */
    public ConnectableObservable<SCStoreStatusEvent> storeEvents;

    public SCDataService(Context context) {
        super();
        this.supportedStores = new HashSet<>();
        this.stores = new HashMap<>();
        // "cold" observable used to emit SCStoreStatusEvents
        this.storeEventSubject = BehaviorSubject.create();
        // turns the "cold" storeEventSubject observable into a "hot" ConnectableObservable
        this.storeEvents = storeEventSubject.publish();
        addDefaultStoreImpls();
        this.context = context;
        initializeDefaultStore();
        initializeFormStore();
        initializeLocationStore();
    }

    private void initializeDefaultStore() {
        Log.d(LOG_TAG, "Creating default store");
        SCStoreConfig defaultStoreConfig = new SCStoreConfig();
        defaultStoreConfig.setName(DefaultStore.NAME);
        defaultStoreConfig.setUniqueID(DefaultStore.NAME);
        defaultStoreConfig.setUri("file://" + DefaultStore.NAME);
        defaultStoreConfig.setType("gpkg");
        defaultStoreConfig.setVersion("1");
        DefaultStore defaultStore = new DefaultStore(context, defaultStoreConfig);
        this.stores.put(defaultStore.getStoreId(), defaultStore);
        // block until store is started
        defaultStore.start().toBlocking().subscribe(new Subscriber<SCStoreStatusEvent>() {
            @Override
            public void onCompleted() {
                Log.d(LOG_TAG, "Registered default store with SCDataService.");
            }

            @Override
            public void onError(Throwable e) {
                Log.e(LOG_TAG, "Couldn't start default store", e);
                System.exit(0);
            }

            @Override
            public void onNext(SCStoreStatusEvent event) {
                Log.w(LOG_TAG, "Shouldn't return onNext");
            }
        });
    }

    private void initializeFormStore() {
        SCStoreConfig formStoreConfig = new SCStoreConfig();
        formStoreConfig.setName(FormStore.NAME);
        formStoreConfig.setUniqueID(FormStore.NAME);
        formStoreConfig.setUri("file://" + FormStore.NAME);
        formStoreConfig.setType("gpkg");
        formStoreConfig.setVersion("1");
        FormStore formStore = new FormStore(context, formStoreConfig);
        this.stores.put(formStore.getStoreId(), formStore);
        // block until store is started
        formStore.start().toBlocking().subscribe(new Subscriber<SCStoreStatusEvent>() {
            @Override
            public void onCompleted() {
                Log.d(LOG_TAG, "Registered form store with SCDataService.");
            }

            @Override
            public void onError(Throwable e) {
                Log.e(LOG_TAG, "Couldn't start form store", e);
                System.exit(0);
            }

            @Override
            public void onNext(SCStoreStatusEvent event) {
                Log.w(LOG_TAG, "Shouldn't return onNext");
            }
        });
    }

    private void initializeLocationStore() {
        SCStoreConfig locationStoreConfig = new SCStoreConfig();
        locationStoreConfig.setName(LocationStore.NAME);
        locationStoreConfig.setUniqueID(LocationStore.NAME);
        locationStoreConfig.setUri("file://" + LocationStore.NAME);
        locationStoreConfig.setType("gpkg");
        locationStoreConfig.setVersion("1");
        LocationStore locationStore = new LocationStore(context, locationStoreConfig);
        this.stores.put(locationStore.getStoreId(), locationStore);
        // block until store is started
        locationStore.start().toBlocking().subscribe(new Subscriber<SCStoreStatusEvent>() {
            @Override
            public void onCompleted() {
                Log.d(LOG_TAG, "Registered location store with SCDataService.");
            }

            @Override
            public void onError(Throwable e) {
                Log.e(LOG_TAG, "Couldn't start form store", e);
                System.exit(0);
            }

            @Override
            public void onNext(SCStoreStatusEvent event) {
                Log.w(LOG_TAG, "Shouldn't return onNext");
            }
        });
    }

    public void addDefaultStoreImpls() {
        this.supportedStores.add(GeoJsonStore.TYPE + "." + "1");
        this.supportedStores.add(GeoJsonStore.TYPE + "." + "1.0");
        this.supportedStores.add(GeoPackageStore.TYPE + "." + "1");
        this.supportedStores.add(GeoPackageStore.TYPE + "." + "1.0");
        this.supportedStores.add(WFSStore.TYPE + "." + "1.1.0");
    }

    public void startStore(final SCDataStore store) {
        if (!store.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)) {
            Log.d(LOG_TAG, "Starting store " + store.getName() + " " + store.getStoreId());
            store.start().subscribe(new Action1<SCStoreStatusEvent>() {
                @Override
                public void call(SCStoreStatusEvent s) {
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable t) {
                    Log.d(LOG_TAG, t.getLocalizedMessage());
                    // onError can happen if we cannot start the store b/c of some error or runtime exception
                    storeEventSubject.onNext(
                            new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_STOPPED, store.getStoreId())
                    );
                }
            }, new Action0() {
                @Override
                public void call() {
                    Log.d(LOG_TAG, "Store " + store.getName() + " is running.");
                    storeEventSubject.onNext(
                            new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_RUNNING, store.getStoreId())
                    );
                }
            });
        }
    }

    /**
     * Closes the stream by calling the subscriber's onComplete() when the specified store has started.
     *
     * @return an Observable that completes when the store is started.
     */
    public Observable<Void> storeStarted(final String storeId) {
        // if the data service already has a running instance of the store, then it's started
        for (SCDataStore store : getActiveStores()) {
            if (storeId.equals(store.getStoreId())) {
                Log.d(LOG_TAG, "Store " + store.getName() + " was already started");
                return Observable.empty();
            }
        }
        // otherwise we wait for the SC_DATA_STORE_RUNNING event for the store.
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> subscriber) {
                        // filter the events
                        storeEventSubject.filter(
                                new Func1<SCStoreStatusEvent, Boolean>() {
                                    @Override
                                    public Boolean call(SCStoreStatusEvent scStoreStatusEvent) {
                                        return scStoreStatusEvent.getStatus()
                                                .equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING) &&
                                                scStoreStatusEvent.getStoreId().equals(storeId);
                                    }
                                }
                        ).subscribe(new Action1<SCStoreStatusEvent>() {
                            @Override
                            public void call(final SCStoreStatusEvent scStoreStatusEvent) {
                                subscriber.onCompleted();
                            }
                        });
                    }
                }
        );
    }


    /**
     * Calling start on the {@link SCDataService} will start all registered {@link SCDataStore}s.
     */
    public void start() {
        Log.d(LOG_TAG, "Starting SCDataService. Starting all registered data stores.");
        super.start();
        for (SCDataStore store : getAllStores()) {
            startStore(store);
        }
        this.setStatus(SCServiceStatus.SC_SERVICE_RUNNING);
    }

    public void addNewStore(SCStoreConfig scStoreConfig) {
        Log.d(LOG_TAG, "Registering new store " + scStoreConfig.getName());
        String key = scStoreConfig.getType() + "." + scStoreConfig.getVersion();
        if (isStoreSupported(key)) {
            if (key.startsWith(GeoJsonStore.TYPE)) {
                Log.d(LOG_TAG, "Registering geojson store " + scStoreConfig.getName() + " with SCDataService.");
                registerStore(new GeoJsonStore(context, scStoreConfig));
            }
            else if (key.startsWith(GeoPackageStore.TYPE)) {
                Log.d(LOG_TAG, "Registering gpkg store " + scStoreConfig.getName() + " with SCDataService.");
                registerStore(new GeoPackageStore(context, scStoreConfig));
            }
            else if (key.startsWith(WFSStore.TYPE)) {
                Log.d(LOG_TAG, "Registering wfs store " + scStoreConfig.getName() + " with SCDataService.");
                registerStore(new WFSStore(context, scStoreConfig));
            }
        }
        else {
            Log.w(LOG_TAG, "Cannot register new store b/c it's unsupported. The unsupported store key was " + key);
        }
        // now persist the store's properties
        SCStoreConfigRepository storeConfigRepository = new SCStoreConfigRepository(context);
        storeConfigRepository.addStoreConfig(scStoreConfig);
    }

    public void addSupportedStoreKey(String key) {
        this.supportedStores.add(key);
    }

    public void removeSupportedStoreKey(String key) {
        this.supportedStores.remove(key);
    }


    public void registerStore(SCDataStore store) {
        this.stores.put(store.getStoreId(), store);
        // if data service is started/running when a new store is added, then we want to start the store
        if (getStatus().equals(SCServiceStatus.SC_SERVICE_RUNNING)) {
            startStore(store);
        }
    }

    public void unregisterStore(SCDataStore store) {
        this.stores.remove(store.getStoreId());
        // if data service is started, and a store is unregister, then we want to stop the store
        if (getStatus().equals(SCServiceStatus.SC_SERVICE_RUNNING)) {
            Log.d(LOG_TAG, "Stopping store " + store.getName());
            store.stop();
        }
    }

    public List<String> getSupportedStoreKeys() {
        return new ArrayList<>(supportedStores);
    }

    public boolean isStoreSupported(String key) {
        return supportedStores.contains(key);
    }

    /**
     * Returns a list of active stores where active is defined as a store that is currently running.
     *
     * @return the list of active data stores
     */
    public List<SCDataStore> getActiveStores() {
        List<SCDataStore> activeStores = new ArrayList<>();
        for (String key : stores.keySet()) {
            SCDataStore scDataStore = stores.get(key);
            if (scDataStore.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)) {
                activeStores.add(scDataStore);
            }
        }
        return activeStores;
    }

    public List<SCDataStore> getAllStores() {
        List<SCDataStore> activeStores = new ArrayList<>();
        for (String key : stores.keySet()) {
            SCDataStore scDataStore = stores.get(key);
            activeStores.add(scDataStore);
        }
        return activeStores;
    }

    /**
     * Returns an active SCDataStore instance that matches the id.
     *
     * @param id
     * @return the active SCDataStore instance or null if one doesn't exist
     */
    public SCDataStore getStoreById(String id) {
        for (SCDataStore store : getAllStores()) {
            if (store.getStoreId().equals(id)) {
                return store;
            }
        }
        return null;
    }

    public Observable<SCSpatialFeature> queryStores(final List<String> storeIds, final SCQueryFilter filter) {
        return Observable.from(getActiveStores())
                .filter(new Func1<SCDataStore, Boolean>() {
                    @Override
                    public Boolean call(final SCDataStore store) {
                        return storeIds.contains(store.getStoreId());
                    }
                })
                .flatMap(new Func1<SCDataStore, Observable<SCSpatialFeature>>() {
                    @Override
                    public Observable<SCSpatialFeature> call(SCDataStore scDataStore) {
                        Log.d(LOG_TAG, "Querying store " + scDataStore.getName());
                        return scDataStore.query(filter);
                    }
                });
    }

    public Observable<SCSpatialFeature> queryAllStores(final SCQueryFilter filter) {
        return Observable.from(getActiveStores())
                .flatMap(new Func1<SCDataStore, Observable<SCSpatialFeature>>() {
                    @Override
                    public Observable<SCSpatialFeature> call(SCDataStore scDataStore) {
                        Log.d(LOG_TAG, "Querying store " + scDataStore.getName());
                        return scDataStore.query(filter);
                    }
                });
    }

    public DefaultStore getDefaultStore() {
        for (SCDataStore store : stores.values()) {
            if (store.getName().equals(DefaultStore.NAME)) {
                return (DefaultStore) store;
            }
        }
        Log.w(LOG_TAG, "Default store was not found!");
        return null;
    }

    public FormStore getFormStore() {
        for (SCDataStore store : stores.values()) {
            if (store.getName().equals(FormStore.NAME)) {
                return (FormStore) store;
            }
        }
        Log.w(LOG_TAG, "Form store was not found!");
        return null;
    }

    public LocationStore getLocationStore() {
        for (SCDataStore store : stores.values()) {
            if (store.getName().equals(LocationStore.NAME)) {
                return (LocationStore) store;
            }
        }
        Log.w(LOG_TAG, "Location store was not found!");
        return null;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}

