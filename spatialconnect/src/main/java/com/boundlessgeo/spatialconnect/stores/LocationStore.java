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
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCPoint;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.mqtt.QoS;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.schema.SCCommand;
import com.boundlessgeo.spatialconnect.schema.SCMessageOuterClass;
import com.boundlessgeo.spatialconnect.services.SCBackendService;
import com.boundlessgeo.spatialconnect.services.SCSensorService;
import com.boundlessgeo.spatialconnect.services.SCServiceStatus;
import com.boundlessgeo.spatialconnect.services.SCServiceStatusEvent;
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

public class LocationStore extends GeoPackageStore implements  SCSpatialStore, SCDataStoreLifeCycle {

    private static final String LOG_TAG = LocationStore.class.getSimpleName();
    private final String LAST_KNOWN_TABLE = "last_known_location";
    private final String ACCURACY_COLUMN = "accuracy";
    private final String TIMESTAMP_COLUMN = "timestamp";
    public static final String NAME = "LOCATION_STORE";

    public LocationStore(Context context, SCStoreConfig scStoreConfig) {
        super(context, scStoreConfig);
    }

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

    public void destroy() {
        super.destroy();
    }

    private void listenForLocationUpdate() {
        SCSensorService.running.subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                if (integer == 1) {
                    SpatialConnect sc = SpatialConnect.getInstance();
                    if (sc.getSensorService() != null) {
                        SpatialConnect.getInstance()
                                .getSensorService()
                                .getLastKnownLocation()
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
                                        scFeatureLocationUpdate.setLayerId(LAST_KNOWN_TABLE);
                                        scFeatureLocationUpdate.getProperties().put(TIMESTAMP_COLUMN, System.currentTimeMillis());
                                        scFeatureLocationUpdate.getProperties().put(ACCURACY_COLUMN, "GPS");

                                        publishLocation(new SCPoint(point));

                                        LocationStore locationStore = SpatialConnect.getInstance().getDataService().getLocationStore();
                                        locationStore.create(scFeatureLocationUpdate).subscribe();
                                    }
                                });
                    }
                }
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

    public Observable<SCSpatialFeature> create(final SCPoint point) {
        point.setLayerId("last_known_location");
        point.setStoreId(getStoreId());
        Log.d(LOG_TAG, "writing new location row " + point.toJson());
        return super.create(point);
    }

    private void publishLocation(final SCPoint point) {
        final SpatialConnect sc = SpatialConnect.getInstance();
        SCSensorService sensorService = sc.getSensorService();
        sensorService.isConnected.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean connected) {
                if (connected) {
                    //make sure backendService is running
                    sc.serviceRunning(SCBackendService.serviceId())
                            .filter(new Func1<SCServiceStatusEvent, Boolean>() {
                                @Override
                                public Boolean call(SCServiceStatusEvent scServiceStatusEvent) {
                                    return scServiceStatusEvent.getStatus()
                                            .equals(SCServiceStatus.SC_SERVICE_RUNNING);
                                }
                            })
                            .subscribe(new Action1<SCServiceStatusEvent>() {
                                @Override
                                public void call(SCServiceStatusEvent scServiceStatusEvent) {
                                    Log.d(LOG_TAG, "posting new location to tracking topic " + point.toJson());
                                    SCMessageOuterClass.SCMessage message = SCMessageOuterClass.SCMessage.newBuilder()
                                            .setAction(SCCommand.NO_ACTION.value())
                                            .setPayload(point.toJson())
                                            .build();
                                    SpatialConnect.getInstance()
                                            .getBackendService()
                                            .publish("/store/tracking", message, QoS.AT_MOST_ONCE);
                                }
                            });
                }
            }
        });
    }

    public Observable<SCSpatialFeature> update(final SCSpatialFeature scSpatialFeature) {
        return Observable.empty();
    }

    @Override
    public Observable<Void> delete(final SCKeyTuple keyTuple) {
        return Observable.empty();
    }

}
