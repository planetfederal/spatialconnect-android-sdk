package com.boundlessgeo.spatialconnect.stores;

import android.content.Context;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.services.SCSensorService;

import rx.Observable;
import rx.functions.Action1;


public class SCRemoteDataStore extends SCDataStore implements SCDataStoreLifeCycle {

    public SCRemoteDataStore(Context context, SCStoreConfig scStoreConfig) {
        super(context, scStoreConfig);
    }

    @Override
    public Observable<SCStoreStatusEvent> start() {
        listenForConnection();
        return null;
    }

    @Override
    public void stop() {
        setStatus(SCDataStoreStatus.SC_DATA_STORE_STOPPED);
    }

    @Override
    public void resume() {
        listenForConnection();
    }

    @Override
    public void pause() {
        setStatus(SCDataStoreStatus.SC_DATA_STORE_PAUSED);
    }

    private void listenForConnection() {
        SCSensorService ss = SpatialConnect.getInstance().getSensorService();
        ss.isConnected.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean connected) {
                if (connected) {
                    setStatus(SCDataStoreStatus.SC_DATA_STORE_RUNNING);
                } else {
                    setStatus(SCDataStoreStatus.SC_DATA_STORE_PAUSED);
                }
            }
        });
    }

}
