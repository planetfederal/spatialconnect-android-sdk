package com.boundlessgeo.spatialconnect.stores;


import rx.Observable;

public interface SCDataStoreLifeCycle
{
    public Observable<SCStoreStatusEvent> start();
    public void stop();
    public void resume();
    public void pause();
}
