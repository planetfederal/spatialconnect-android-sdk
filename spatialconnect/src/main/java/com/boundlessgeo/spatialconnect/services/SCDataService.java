package com.boundlessgeo.spatialconnect.services;

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
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subjects.PublishSubject;

public class SCDataService extends SCService
{
    private SCServiceStatus status;
    private Set<String> supportedStores; // the strings are store keys: type.version
    private Map<String, SCDataStore> stores;
    private boolean storesStarted;
    private final String DATA_SERVICE = "DataService";
    protected PublishSubject<SCStoreStatusEvent> storeEventSubject;
    public ConnectableObservable<SCStoreStatusEvent> storeEvents;

    public SCDataService()
    {
        super();
        this.supportedStores = new HashSet<>();
        this.stores = new HashMap<>();
        this.storeEventSubject = PublishSubject.create();
        this.storeEvents = storeEventSubject.publish();
        addDefaultStoreImpls();
    }

    public void addDefaultStoreImpls()
    {
        this.supportedStores.add(GeoJsonStore.versionKey());
        this.supportedStores.add(GeoPackageStore.versionKey());
    }

    public void startAllStores()
    {
        Set<String> s = stores.keySet();
        final int storesCount = s.size();

        storeEvents.filter(new Func1<SCStoreStatusEvent, Boolean>() {
            @Override
            public Boolean call(SCStoreStatusEvent scStoreStatusEvent) {
                return scStoreStatusEvent.getStatus() == SCDataStoreStatus.SC_DATA_STORE_RUNNING;
            }
        }).buffer(storesCount).take(1).subscribe(new Action1<List<SCStoreStatusEvent>>() {
            @Override
            public void call(List<SCStoreStatusEvent> l) {
                SCStoreStatusEvent se = new SCStoreStatusEvent(SCDataStoreStatus.SC_DATA_SERVICE_ALLSTORESSTARTED, null);
                SCDataService.this.storeEventSubject.onNext(se);
            }
        });

        for (String key : s)
        {
            Object obj = stores.get(key);
            if (obj instanceof SCDataStoreLifeCycle)
            {
                this.startStore((SCDataStoreLifeCycle) stores.get(key));
            }
        }
    }

    private void startStore(SCDataStoreLifeCycle s) {
        s.start().subscribe(new Action1<SCStoreStatusEvent>() {
            @Override
            public void call(SCStoreStatusEvent s) {
                System.out.println(s.getStatus() + " " + s.getStoreId());
            }
        },new Action1<Throwable>(){
            @Override
            public void call(Throwable t) {
                System.out.println(t.getLocalizedMessage());
            }
        },new Action0() {
            @Override
            public void call() {
                System.out.println();
            }
        });
    }

    public Observable<SCStoreStatusEvent> allStoresStartedObs()
    {
        Boolean allStarted = true;
        for (String key : stores.keySet()) {
            SCDataStore store = stores.get(key);
            if (store.getStatus() != SCDataStoreStatus.SC_DATA_STORE_RUNNING) {
                allStarted = false;
                break;
            }
        }

        if (allStarted) {
            return Observable.empty();
        } else {
            return storeEvents.filter(
                    new Func1<SCStoreStatusEvent, Boolean>() {
                        @Override
                        public Boolean call(SCStoreStatusEvent scStoreStatusEvent) {
                            return (scStoreStatusEvent.getStatus() ==
                                    SCDataStoreStatus.SC_DATA_SERVICE_ALLSTORESSTARTED);
                        }
                    }
            );
        }
    }

    public void start()
    {
        super.start();
        startAllStores();
        this.storesStarted = true;
    }

    public void addSupportedStoreKey(String key)
    {
        this.supportedStores.add(key);
    }

    public void removeSupportedStoreKey(String key)
    {
        this.supportedStores.remove(key);
    }


    public void registerStore(SCDataStore store) {
        this.stores.put(store.getStoreId(), store);
    }

    public void unregisterStore(SCDataStore store)
    {
        this.stores.remove(store.getStoreId());
    }

    public List<String> getSupportedStoreKeys()
    {
        return new ArrayList<>(supportedStores);
    }

    public boolean isStoreSupported(String key)
    {
        return supportedStores.contains(key);
    }

    /**
     * Returns a list of active stores where active is defined as a store that is currently running.
     *
     * @return the list of active data stores
     */
    public List<SCDataStore> getActiveStores()
    {
        List<SCDataStore> activeStores = new ArrayList<>();
        for (String key : stores.keySet())
        {
            SCDataStore scDataStore = stores.get(key);
            if (scDataStore.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING))
            {
                activeStores.add(scDataStore);
            }
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
        for (SCDataStore store : getActiveStores()) {
            if (store.getStoreId().equals(id)) {
                return store;
            }
        }
        return null;
    }


    /*
     * StoreQueryDAO is an inner class
     */
    public Observable<SCSpatialFeature> queryStore(String id, SCQueryFilter filter)
    {
        StoreQueryDAO dao = new StoreQueryDAO(id, filter);
        Observable<SCSpatialFeature> queryresults =
                Observable
                        .just(dao)
                        .flatMap(new Func1<StoreQueryDAO, Observable<SCSpatialFeature>>()
                        {
                            @Override
                            public Observable<SCSpatialFeature> call(StoreQueryDAO dao)
                            {
                                Observable<SCSpatialFeature> results;
                                SCDataStore ds = stores.get(dao.getId());
                                if(ds != null && ds.getStatus() == SCDataStoreStatus.SC_DATA_STORE_RUNNING)
                                {
                                    SCSpatialStore sp = (SCSpatialStore) ds;
                                    results = sp.query(dao.filter);
                                }
                                else
                                {
                                    results = Observable.empty();
                                }
                                return results;
                            }
                        });
        return queryresults;
    }

    public Observable<SCSpatialFeature> queryAllStores(SCQueryFilter filter) {
        Observable<SCSpatialFeature> queryResults =
                Observable
                        .just(filter)
                        .flatMap(new Func1<SCQueryFilter, Observable<SCSpatialFeature>>() {
                            @Override
                            public Observable<SCSpatialFeature> call(SCQueryFilter filter) {
                                List<SCDataStore> runningStores = getActiveStores();

                                List<Observable<SCSpatialFeature>> queryResults = new ArrayList<>();
                                for (SCDataStore ds : runningStores) {
                                    SCSpatialStore sp = (SCSpatialStore) ds;
                                    queryResults.add(sp.query(filter));
                                }

                                Observable<SCSpatialFeature> results = Observable.merge(queryResults);
                                return results;
                            }
                        });
        return queryResults;
    }

    class StoreQueryDAO
    {
        private String id;
        private SCQueryFilter filter;

        public StoreQueryDAO(String id, SCQueryFilter filter)
        {
            this.id = id;
            this.filter = filter;
        }

        public String getId()
        {
            return id;
        }

        public SCQueryFilter getFilter()
        {
            return filter;
        }
    }
}

