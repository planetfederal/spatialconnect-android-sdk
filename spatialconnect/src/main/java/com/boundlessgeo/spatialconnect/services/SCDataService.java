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
package com.boundlessgeo.spatialconnect.services;

import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.stores.FormStore;
import com.boundlessgeo.spatialconnect.stores.GeoJsonStore;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.ISCSpatialStore;
import com.boundlessgeo.spatialconnect.stores.ISyncableStore;
import com.boundlessgeo.spatialconnect.stores.LocationStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreLifeCycle;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCRasterStore;
import com.boundlessgeo.spatialconnect.stores.SCRemoteDataStore;
import com.boundlessgeo.spatialconnect.stores.SCStoreStatusEvent;
import com.boundlessgeo.spatialconnect.stores.WFSStore;
import com.boundlessgeo.spatialconnect.style.SCStyle;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;

import static java.util.Arrays.asList;

/**
 * The SCDataService is responsible for starting, stopping, and managing access to {@link SCDataStore} instances.  This
 * includes exposing methods to query across multiple SCDataStore instances.
 */
public class SCDataService extends SCService implements SCServiceLifecycle {

    private static final String LOG_TAG = SCDataService.class.getSimpleName();
    private static final String SERVICE_NAME = "SC_DATA_SERVICE";
    private Set<String> supportedStores; // the strings are store keys: type.version
    private Map<String, SCDataStore> stores;
    private Map<String, Class> supportedStoreImpls;
    private Context context;
    private SCSensorService sensorService;

    /**
     * The storeEventSubject is like an internal event bus, its job is to receive {@link SCStoreStatusEvent}s
     * published by a {@link SCDataStore}.
     */
    private BehaviorSubject<SCStoreStatusEvent> storeEventSubject;

    /**
     * This is the Observable that subscribers in your app will subscribe to so that they can receive {@link
     * SCStoreStatusEvent}s.  Subscribers must explicitly call {@link rx.observables.ConnectableObservable#connect()}
     * connect to start receiving events b/c storeEvents is a ConnectableObservable. We use a ConnectableObservable
     * so that multiple subscribers can connect before beginning to emit the events about the {@link SCDataStore}s.
     */
    public ConnectableObservable<SCStoreStatusEvent> storeEvents;
    public BehaviorSubject<Boolean> hasStores = BehaviorSubject.create(false);

    public SCDataService(Context context) {
        super();
        this.supportedStores = new HashSet<>();
        this.stores = new HashMap<>();
        this.supportedStoreImpls = new HashMap<>();

        // "cold" observable used to emit SCStoreStatusEvents
        this.storeEventSubject = BehaviorSubject.create();
        // turns the "cold" storeEventSubject observable into a "hot" ConnectableObservable
        this.storeEvents = storeEventSubject.publish();
        addDefaultStoreImpls();
        this.context = context;
        initializeFormStore();
        initializeLocationStore();
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

    public void registerAndStartStoreByConfig(SCStoreConfig config) {
        if (registerStoreByConfig(config)) {
            SCDataStore store = stores.get(config.getUniqueID());
            if (store != null) {
                ((SCDataStoreLifeCycle) store).start();
            }
        }
    }

    public void registerStore(SCDataStore store) {
        this.stores.put(store.getStoreId(), store);
        // if data service is started/running when a new store is added, then we want to start the store
        if (getStatus().equals(SCServiceStatus.SC_SERVICE_RUNNING)) {
            startStore(store);
        }
    }

    public boolean registerStoreByConfig(SCStoreConfig scStoreConfig) {
        String key = scStoreConfig.getType() + "." + scStoreConfig.getVersion();
        Class store = getSupportedStoreByKey(key);
        if (store != null) {
            ArrayNode styleArray = scStoreConfig.getStyle();
            SCStyle storeStyle = (styleArray != null && styleArray.size() > 0) ?
                    new SCStyle(styleArray) : new SCStyle();

            if (key.startsWith(GeoJsonStore.TYPE)) {
                Log.d(LOG_TAG, "Registering geojson store " + scStoreConfig.getName() + " with SCDataService.");
                registerStore(new GeoJsonStore(context, scStoreConfig, storeStyle));
            }
            else if (key.startsWith(GeoPackageStore.TYPE)) {
                Log.d(LOG_TAG, "Registering gpkg store " + scStoreConfig.getName() + " with SCDataService.");
                registerStore(new GeoPackageStore(context, scStoreConfig, storeStyle));
            }
            else if (key.startsWith(WFSStore.TYPE)) {
                Log.d(LOG_TAG, "Registering wfs store " + scStoreConfig.getName() + " with SCDataService.");
                registerStore(new WFSStore(context, scStoreConfig, storeStyle));
            }

            Log.d(LOG_TAG,"returning true from register store by config");
            return true;
        }
        else {
            Log.w(LOG_TAG, "Cannot register new store b/c it's unsupported. The unsupported store key was " + key);
            return false;
        }

    }

    public void unregisterStore(SCDataStore store) {
        this.stores.remove(store.getStoreId());
        if (getStatus().equals(SCServiceStatus.SC_SERVICE_RUNNING)) {
            Log.d(LOG_TAG, "Stopping store " + store.getName());
            stopStore(store);
            destroyStore(store);
        }
    }

    public void updateStore(SCDataStore store) {
        stopStore(store);
        stores.put(store.getStoreId(), store);
        startStore(store);
    }

    public boolean updateStoresByConfig(SCStoreConfig scStoreConfig) {
        String key  = String.format("%s.%s", scStoreConfig.getType(), scStoreConfig.getVersion());
        Class store = getSupportedStoreByKey(key);
        if (store != null) {

            SCDataStore currentStore = stores.get(scStoreConfig.getUniqueID());
            SCDataStore updatedStore = null;

            ArrayNode styleArray = scStoreConfig.getStyle();
            SCStyle storeStyle = (styleArray != null && styleArray.size() > 0) ?
                    new SCStyle(styleArray) : new SCStyle();

            if (key.startsWith(GeoJsonStore.TYPE)) {
                Log.d(LOG_TAG, "Updating geojson store " + scStoreConfig.getName() + " with SCDataService.");
                updatedStore = new GeoJsonStore(context, scStoreConfig, storeStyle);
            }
            else if (key.startsWith(GeoPackageStore.TYPE)) {
                Log.d(LOG_TAG, "Updating gpkg store " + scStoreConfig.getName() + " with SCDataService.");
                updatedStore = new GeoPackageStore(context, scStoreConfig, storeStyle);
            }
            else if (key.startsWith(WFSStore.TYPE)) {
                Log.d(LOG_TAG, "Updating wfs store " + scStoreConfig.getName() + " with SCDataService.");
                updatedStore = new WFSStore(context, scStoreConfig, storeStyle);
            }

            if (currentStore == null) {
                Log.e(LOG_TAG, String.format("Store %s does not exists", scStoreConfig.getUniqueID()));
                return false;
            } else {
                if (updatedStore != null) {
                    updateStore(updatedStore);
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            Log.e(LOG_TAG,
                    String.format("The store you tried to start:%s doesn't have a support implementation"
                            , key));
            return false;
        }
    }

    public Class getSupportedStoreByKey(String key) {
        return supportedStoreImpls.get(key);
    }



    public List<SCDataStore> getStoreList() {
        List<SCDataStore> activeStores = new ArrayList<>();
        for (String key : stores.keySet()) {
            SCDataStore scDataStore = stores.get(key);
            activeStores.add(scDataStore);
        }
        return activeStores;
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

    public List<SCDataStore> getStoresRaster() {
        List<SCDataStore> rasterStores = new ArrayList<>();
        for (String key : stores.keySet()) {
            SCDataStore scDataStore = stores.get(key);
            if (scDataStore.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING) &&
                    scDataStore instanceof SCRasterStore) {
                rasterStores.add(scDataStore);
            }
        }
        return rasterStores;
    }

    /**
     * Returns an active SCDataStore instance that matches the id.
     *
     * @param id
     * @return the active SCDataStore instance or null if one doesn't exist
     */
    public SCDataStore getStoreByIdentifier(String id) {
        for (SCDataStore store : getStoreList()) {
            if (store.getStoreId().equals(id)) {
                return store;
            }
        }
        return null;
    }

    public List<SCDataStore> getISCSpatialStoresArray() {
        return getISCSpatialStoresArray(true);
    }

    public Observable<SCDataStore> getISCSpatialStores() {
        return getISCSpatialStores(true);
    }

    public Observable<SCDataStore> getISCSpatialStores(final Boolean onlyRunning) {
        return Observable.from(stores.values())
                .filter(new Func1<SCDataStore, Boolean>() {
                    @Override
                    public Boolean call(final SCDataStore store) {
                        return (store instanceof ISCSpatialStore);
                    }
                })
                .filter(new Func1<SCDataStore, Boolean>() {
                    @Override
                    public Boolean call(final SCDataStore store) {
                        if (!onlyRunning) {
                            return true;
                        } else {
                            return store.getStatus() == SCDataStoreStatus.SC_DATA_STORE_RUNNING;
                        }
                    }
                });
    }

    public Observable<SCDataStore> getISyncableStores() {
        List<SCDataStore> syncableStores = new ArrayList<>();
        for (String key : stores.keySet()) {
            SCDataStore scDataStore = stores.get(key);
            if (scDataStore.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING) &&
                    scDataStore instanceof ISyncableStore && scDataStore.getName().equalsIgnoreCase(LocationStore.NAME)) {
                syncableStores.add(scDataStore);
            }
        }

        return Observable.from(syncableStores)
                .filter(new Func1<SCDataStore, Boolean>() {
                    @Override
                    public Boolean call(final SCDataStore store) {
                        return store.getStatus() == SCDataStoreStatus.SC_DATA_STORE_RUNNING;
                    }
                });
    }

    public List<SCDataStore> getISCSpatialStoresArray(final Boolean onlyRunning) {
        return getISCSpatialStores(onlyRunning).toList().toBlocking().first();
    }

    public Observable<SCSpatialFeature> queryAllStores(final SCQueryFilter filter) {
        return Observable.from(getISCSpatialStoresArray())
                .flatMap(new Func1<SCDataStore, Observable<SCSpatialFeature>>() {
                    @Override
                    public Observable<SCSpatialFeature> call(SCDataStore scDataStore) {
                        Log.d(LOG_TAG, "Querying store " + scDataStore.getName());
                        return ((ISCSpatialStore) scDataStore).query(filter);
                    }
                });
    }

    public Observable<SCSpatialFeature> queryStores(List<SCDataStore> stores, final SCQueryFilter filter) {
        final List<String> storeIds = Observable.from(stores)
                .map(new Func1<SCDataStore, String>() {
                    @Override
                    public String call(SCDataStore scDataStore) {
                        return scDataStore.getStoreId();
                    }
                }).toList().toBlocking().first();

        return queryStoresByIds(storeIds,filter);
    }

    public Observable<SCSpatialFeature> queryStoresByIds(final List<String> storeIds, final SCQueryFilter filter) {

        return getISCSpatialStores().filter(new Func1<SCDataStore, Boolean>() {
            @Override
            public Boolean call(SCDataStore scDataStore) {
                    return storeIds.contains(scDataStore.getStoreId());
                }
            })
            .flatMap(new Func1<SCDataStore, Observable<SCSpatialFeature>>() {
                @Override
                public Observable<SCSpatialFeature> call(SCDataStore scDataStore) {
                    Log.d(LOG_TAG, "Querying store " + scDataStore.getName());
                    return ((ISCSpatialStore) scDataStore).query(filter);
                }
            });
    }

    public Observable<SCSpatialFeature> queryStoreById(String storeId, final SCQueryFilter filter) {
        SCDataStore store = getStoreByIdentifier(storeId);
        Log.d(LOG_TAG, "Querying store by Id with Filter:  " + store.getName());
        return ((ISCSpatialStore) store).query(filter);
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

    public BehaviorSubject<SCStoreStatusEvent> getStoreEvents() {
        return storeEventSubject;
    }

    /**
     * Calling start on the {@link SCDataService} will start all registered {@link SCDataStore}s.
     */
    @Override
    public boolean start(Map<String, SCService> deps) {
        Log.d(LOG_TAG, "Starting SCDataService. Starting all registered data stores.");
        sensorService = (SCSensorService)deps.get(SCSensorService.serviceId());
        startAllStores();
        setupSubscriptions();
        return super.start(deps);
    }

    @Override
    public boolean stop() {
        stopAllStores();
        stores.clear();
        hasStores.onNext(false);
        return super.stop();
    }

    @Override
    public String getId() {
        return SERVICE_NAME;
    }

    @Override
    public List<String> getRequires() {
        return asList(SCSensorService.serviceId());
    }

    private void startAllStores() {
        for (SCDataStore store : getStoreList()) {
            startStore(store);
        }
    }

    private void stopAllStores() {
        for (SCDataStore store : getStoreList()) {
            stopStore(store);
        }
    }

    private void startStore(final SCDataStore store) {
        if (!store.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)) {
            ((SCDataStoreLifeCycle) store).start()
                    .observeOn(AndroidSchedulers.mainThread())
                    .sample(2, TimeUnit.SECONDS)
                    .subscribe(new Action1<SCStoreStatusEvent>() {
                        @Override
                        public void call(SCStoreStatusEvent s) {
                            storeEventSubject.onNext(
                                    new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_DOWNLOAD_PROGRESS, store.getStoreId()));
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable t) {
                            String errorMsg = (t != null) ? t.getLocalizedMessage() : "no message available";
                            Log.e(LOG_TAG,"Unable to start store: " + store.getStoreId() + " with error: " + errorMsg);
                            // onError can happen if we cannot start the store b/c of some error or runtime exception
                            storeEventSubject.onNext(
                                    new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_START_FAILED, store.getStoreId())
                            );
                        }
                    }, new Action0() {
                        @Override
                        public void call() {
                            Log.d(LOG_TAG, "Store " + store.getName() + " is running.");
                            store.setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                            storeEventSubject.onNext(
                                    new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_RUNNING, store.getStoreId())
                            );

                            hasStores.onNext(true);
                        }
                    });
        }
    }

    private void stopStore(final SCDataStore store) {
        if (store.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)) {
            Log.d(LOG_TAG, "Stopping store " + store.getName() + " " + store.getStoreId());
            ((SCDataStoreLifeCycle) store).stop();
            stores.remove(store);
            storeEventSubject.onNext(
                    new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_REMOVED, store.getStoreId()));
            if (stores.size() > 0) {
                hasStores.onNext(true);
            }
        }
    }

    private void destroyStore(final SCDataStore store) {
        if (store.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)) {
            ((SCDataStoreLifeCycle) store).destroy();
            hasStores.onNext(stores.size() > 0);
            storeEventSubject.onNext(
                    new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_REMOVED, store.getStoreId()));
        }
    }

    private void pauseRemoteStores() {
        for (SCDataStore store : stores.values()) {
            if (store instanceof SCRemoteDataStore && store.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)) {
                ((SCRemoteDataStore) store).pause();
                storeEventSubject.onNext(
                        new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_PAUSED, store.getStoreId()));
            }
        }
    }

    private void resumeRemoteStores() {
        for (SCDataStore store : stores.values()) {
            if (store instanceof SCRemoteDataStore && store.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_PAUSED)) {
                ((SCRemoteDataStore) store).resume();
                storeEventSubject.onNext(
                        new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_RESUMED, store.getStoreId()));
            }
        }
    }

    private void setupSubscriptions() {
        sensorService.isConnected().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean connected) {
                if (connected) {
                    resumeRemoteStores();
                } else {
                    pauseRemoteStores();
                }
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
        Log.e(LOG_TAG, "Location store added to stores...");
    }

    private void addDefaultStoreImpls() {
        supportedStoreImpls.put(GeoJsonStore.getVersionKey() ,GeoJsonStore.class);
        supportedStoreImpls.put(GeoPackageStore.getVersionKey() ,GeoPackageStore.class);
        supportedStoreImpls.put(WFSStore.getVersionKey() ,WFSStore.class);
    }

    public static String serviceId() {
        return SERVICE_NAME;
    }
}

