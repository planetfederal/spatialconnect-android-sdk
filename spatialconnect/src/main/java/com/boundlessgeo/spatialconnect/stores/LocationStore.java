/**
 * Copyright 2016 Boundless http://boundlessgeo.com
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
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.boundlessgeo.spatialconnect.stores;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.db.SCGpkgFeatureSource;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCPoint;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.style.SCStyle;
import com.boundlessgeo.spatialconnect.sync.SyncItem;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Store that saves the device locations in a GeoPackage vector feature table.
 */
public class LocationStore extends GeoPackageStore implements ISCSpatialStore, SCDataStoreLifeCycle {

    private static final String LOG_TAG = LocationStore.class.getSimpleName();
    private final String LAST_KNOWN_TABLE = "last_known_location";
    private final String ACCURACY_COLUMN = "accuracy";
    private final String TIMESTAMP_COLUMN = "timestamp";
    public static final String NAME = "LOCATION_STORE";

    public LocationStore(Context context, SCStoreConfig scStoreConfig) {
        this(context, scStoreConfig, null);
    }

    public LocationStore(Context context, SCStoreConfig scStoreConfig, SCStyle style) {
        super(context, scStoreConfig, style);
    }

    public Observable<SCSpatialFeature> create(final SCPoint point) {
        point.setLayerId("last_known_location");
        point.setStoreId(getStoreId());
        Log.d(LOG_TAG, "writing new location row " + point.toJson());
        return super.create(point);
    }

    @Override
    public Observable<SCStoreStatusEvent> start() {
        Observable<SCStoreStatusEvent> storeStatusEvent;
        storeStatusEvent = super.start();
        return storeStatusEvent.doOnCompleted(new Action0() {
            @Override
            public void call() {
                //when store started add default config
                Map<String, String> typeDefs = new HashMap<>();
                typeDefs.put(ACCURACY_COLUMN,"TEXT");
                typeDefs.put(TIMESTAMP_COLUMN,"INTEGER");
                addLayer(LAST_KNOWN_TABLE, typeDefs);
                listenForLocationUpdate();
            }
        });
    }

    @Override
    public Observable<SCSpatialFeature> query(final SCQueryFilter scFilter) {
        return Observable.empty();
    }

    @Override
    public Observable<SCSpatialFeature> queryById(final SCKeyTuple keyTuple) {
        return Observable.empty();
    }

    @Override
    public Observable<SCSpatialFeature> update(final SCSpatialFeature scSpatialFeature) {
        return Observable.empty();
    }

    @Override
    public Observable<Void> delete(final SCKeyTuple keyTuple) {
        return Observable.empty();
    }

    @Override
    public String syncChannel() {
        return "/store/tracking";
    }

    @Override
    public Map<String, Object> generateSendPayload(SCSpatialFeature scSpatialFeature) {
        return scSpatialFeature.toMap();
    }

    //only send the last unSent location record
    @Override
    public Observable<SyncItem> unSent() {
        Observable<SyncItem> unSentFeatures = gpkg.unSent().takeLast(1);
        return unSentFeatures.map(new Func1<SyncItem, SyncItem>() {
            @Override
            public SyncItem call(SyncItem syncItem) {
                syncItem.getFeature().setStoreId(storeId);
                return syncItem;
            }
        });
    }

    @Override
    public void updateAuditTable(SCSpatialFeature scSpatialFeature) {
        SCGpkgFeatureSource fs = gpkg.getFeatureSourceByName(scSpatialFeature.getLayerId());
        //since only taking last location, update all others before scSpatialFeature
        fs.updateAuditTableFromLatest(scSpatialFeature);
    }

    private void listenForLocationUpdate() {
        final SpatialConnect sc = SpatialConnect.getInstance();
        final SCSensorService ss = sc.getSensorService();
        ss.isConnected().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean connected) {
                if (connected) {
                    if (ss != null) {
                        ss.getLastKnownLocation()
                                .subscribe(new Action1<Location>() {
                                    @Override
                                    public void call(Location location) {
                                        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 0);
                                        Geometry point = geometryFactory.createPoint(
                                                new Coordinate(
                                                        location.getLongitude(),
                                                        location.getLatitude(),
                                                        location.getAltitude()
                                                )
                                        );
                                        SCGeometry scFeatureLocationUpdate = new SCGeometry(point);
                                        scFeatureLocationUpdate.setStoreId(getStoreId());
                                        scFeatureLocationUpdate.setLayerId(LAST_KNOWN_TABLE);
                                        scFeatureLocationUpdate.getProperties().put(TIMESTAMP_COLUMN, System.currentTimeMillis());
                                        scFeatureLocationUpdate.getProperties().put(ACCURACY_COLUMN, "GPS");

                                        create(scFeatureLocationUpdate).subscribe();
                                    }
                                });
                    }
                }
            }
        });
    }

}
