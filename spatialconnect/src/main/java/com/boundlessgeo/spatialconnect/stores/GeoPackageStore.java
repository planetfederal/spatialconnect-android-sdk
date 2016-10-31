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
import com.boundlessgeo.spatialconnect.db.SCGpkgFeatureSource;
import com.boundlessgeo.spatialconnect.geometries.SCPolygon;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.tiles.SCGpkgTileSource;
import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rx.Observable;

/**
 * Provides capabilities for interacting with a single GeoPackage.
 */
public class GeoPackageStore extends SCDataStore implements SCSpatialStore, SCDataStoreLifeCycle, SCRasterStore {

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
        this.setAdapter(new GeoPackageAdapter(context, scStoreConfig));
    }

    public List<String> layers() {
        List<String> allLayers = new ArrayList<String>();
        allLayers.addAll(this.vectorLayers());
        allLayers.addAll(this.rasterLayers());
        return allLayers;
    }

    public List<String> vectorLayers() {
        GeoPackageAdapter adapter = (GeoPackageAdapter) this.getAdapter();
        Map<String, SCGpkgFeatureSource> fs =  adapter.getFeatureSources();
        List<String> layerNames = new ArrayList<>(fs.keySet());
        return layerNames;
    }

    public List<String> rasterLayers() {
        GeoPackageAdapter adapter = (GeoPackageAdapter) this.getAdapter();
        Map<String, SCGpkgTileSource> fs =  adapter.getTileSources();
        List<String> layerNames = new ArrayList<>(fs.keySet());
        return layerNames;
    }

    public void addLayer(String layer, Map<String,String>  fields) {
        ((GeoPackageAdapter) getAdapter()).addLayer(layer, fields);
    }

    public void deleteLayer(String layer) {
        ((GeoPackageAdapter) getAdapter()).deleteLayer(layer);
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

        return storeInstance.getAdapter().connect();
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

    @Override
    public void overlayFromLayer(String layerName, GoogleMap map) {
        ((GeoPackageAdapter) getAdapter()).overlayFromLayer(layerName, map);
    }

    @Override
    public SCPolygon getCoverage() {
        return null;
    }

}
