package com.boundlessgeo.spatialconnect.stores;


public interface SCDataStoreLifeCycle
{
    public void start();
    public void stop();
    public void resume();
    public void pause();
}
