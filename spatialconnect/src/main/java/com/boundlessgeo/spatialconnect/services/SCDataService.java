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

import android.util.Log;

import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.stores.GeoJsonStore;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreLifeCycle;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCSpatialStore;
import com.boundlessgeo.spatialconnect.stores.SCStoreStatusEvent;

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
    private SCServiceStatus status;
    private Set<String> supportedStores; // the strings are store keys: type.version
    private Map<String, SCDataStore> stores;
    private boolean storesStarted;
    private final String LOG_TAG = "SCDataService";

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

    public SCDataService() {
        super();
        this.supportedStores = new HashSet<>();
        this.stores = new HashMap<>();
        // "cold" observable used to emit SCStoreStatusEvents
        this.storeEventSubject = BehaviorSubject.create();
        // turns the "cold" storeEventSubject observable into a "hot" ConnectableObservable
        this.storeEvents = storeEventSubject.publish();
        addDefaultStoreImpls();
    }

    public void addDefaultStoreImpls() {
        this.supportedStores.add(GeoJsonStore.versionKey());
        this.supportedStores.add(GeoPackageStore.versionKey());
    }

    private void startAllStores() {
        Set<String> s = stores.keySet();
        final int storesCount = s.size();

        storeEventSubject.filter(new Func1<SCStoreStatusEvent, Boolean>() {
            @Override
            public Boolean call(SCStoreStatusEvent scStoreStatusEvent) {
                Log.d(LOG_TAG, "This store is now running: " + scStoreStatusEvent.getStoreId());
                return scStoreStatusEvent.getStatus() == SCDataStoreStatus.SC_DATA_STORE_RUNNING;
            }
        })
                .buffer(storesCount).take(1).subscribe(new Action1<List<SCStoreStatusEvent>>() {
            @Override
            public void call(List<SCStoreStatusEvent> l) {
                // when all stores have started, send the SC_DATA_SERVICE_ALLSTORESSTARTED status
                SCStoreStatusEvent se = new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_SERVICE_ALLSTORESSTARTED, null);
                storeEventSubject.onNext(se);
            }
        });

        for (String key : s) {
            Object obj = stores.get(key);
            if (obj instanceof SCDataStoreLifeCycle) {
                this.startStore((SCDataStoreLifeCycle) stores.get(key));
            }
        }
    }

    private void startStore(final SCDataStoreLifeCycle s) {
        SCDataStore dataStore = (SCDataStore) s;
        s.start().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent s) {
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                Log.d(LOG_TAG, t.getLocalizedMessage());
                // onError can happen if we cannot start the store b/c of some error or runtime exception
                storeEventSubject.onNext(
                        new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_STOPPED, ((SCDataStore) s).getStoreId())
                );
            }
        }, new Action0() {
            @Override
            public void call() {
                storeEventSubject.onNext(
                        new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_STORE_RUNNING, ((SCDataStore) s).getStoreId())
                );
            }
        });
    }

    /**
     * Closes the stream by calling  the subscriber's onComplete() when all stores have been started.
     *
     * @return an Observable that completes when all stores are started.
     */
    public Observable<SCStoreStatusEvent> allStoresStartedObs() {
        return Observable.create(
                new Observable.OnSubscribe<SCStoreStatusEvent>() {
                    @Override
                    public void call(final Subscriber<? super SCStoreStatusEvent> subscriber) {
                        // filter the events
                        storeEventSubject.filter(
                                new Func1<SCStoreStatusEvent, Boolean>() {
                                    @Override
                                    public Boolean call(SCStoreStatusEvent scStoreStatusEvent) {
                                        return (scStoreStatusEvent.getStatus() ==
                                                SCDataStoreStatus.SC_DATA_SERVICE_ALLSTORESSTARTED);
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
        super.start();
        startAllStores();
        this.storesStarted = true;
    }

    public void addSupportedStoreKey(String key) {
        this.supportedStores.add(key);
    }

    public void removeSupportedStoreKey(String key) {
        this.supportedStores.remove(key);
    }


    public void registerStore(SCDataStore store) {
        this.stores.put(store.getStoreId(), store);
    }

    public void unregisterStore(SCDataStore store) {
        this.stores.remove(store.getStoreId());
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


    /*
     * StoreQueryDAO is an inner class
     */
    public Observable<SCSpatialFeature> queryStore(String id, SCQueryFilter filter) {
        StoreQueryDAO dao = new StoreQueryDAO(id, filter);
        Observable<SCSpatialFeature> queryresults =
                Observable
                        .just(dao)
                        .flatMap(new Func1<StoreQueryDAO, Observable<SCSpatialFeature>>() {
                            @Override
                            public Observable<SCSpatialFeature> call(StoreQueryDAO dao) {
                                Observable<SCSpatialFeature> results;
                                SCDataStore ds = stores.get(dao.getId());
                                if (ds != null && ds.getStatus() == SCDataStoreStatus.SC_DATA_STORE_RUNNING) {
                                    SCSpatialStore sp = ds;
                                    results = sp.query(dao.filter);
                                }
                                else {
                                    results = Observable.empty();
                                }
                                return results;
                            }
                        });
        return queryresults;
    }

    public Observable<SCSpatialFeature> queryAllStores(final SCQueryFilter filter) {
        Observable<SCSpatialFeature> queryResults =
                Observable
                        .just(filter)
                        .flatMap(new Func1<SCQueryFilter, Observable<SCSpatialFeature>>() {
                            @Override
                            public Observable<SCSpatialFeature> call(SCQueryFilter filter) {
                                List<SCDataStore> runningStores = getActiveStores();

                                List<Observable<SCSpatialFeature>> queryResults = new ArrayList<>();
                                for (SCDataStore ds : runningStores) {
                                    SCSpatialStore sp = ds;
                                    queryResults.add(sp.query(filter));
                                }

                                Observable<SCSpatialFeature> results = Observable.merge(queryResults);
                                return results;
                            }
                        });
        return queryResults;
    }

    class StoreQueryDAO {
        private String id;
        private SCQueryFilter filter;

        public StoreQueryDAO(String id, SCQueryFilter filter) {
            this.id = id;
            this.filter = filter;
        }

        public String getId() {
            return id;
        }

        public SCQueryFilter getFilter() {
            return filter;
        }
    }
}

