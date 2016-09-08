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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import java.util.HashMap;
import java.util.Map;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;

public class LocationStore extends GeoPackageStore {

    public static final String NAME = "LOCATION_STORE";
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 0);

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
                typeDefs.put("accuracy","TEXT");
                typeDefs.put("timestamp","INTEGER");
                addLayer("last_known_location", typeDefs);
                // setup subscription to write location updates to last_known_location
                SpatialConnect.getInstance()
                    .getSensorService()
                    .getLastKnownLocation()
                    .subscribe(new Action1<Location>() {
                        @Override public void call(Location location) {
                            Geometry point = geometryFactory.createPoint(
                                new Coordinate(location.getLongitude(), location.getLatitude()));
                            SCGeometry newLocation = new SCGeometry(point);
                            Log.d("LocationStore", "writing new location " + newLocation.toJson());
                            create(newLocation);
                        }
                    });
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

        Observable<SCSpatialFeature> spatialFeature = super.create(point);

        return spatialFeature.doOnCompleted(new Action0() {
            @Override
            public void call() {
                SCMessageOuterClass.SCMessage.Builder builder =
                        SCMessageOuterClass.SCMessage.newBuilder();
                builder.setAction(SCCommand.NO_ACTION.value())
                        .setPayload(point.toJson())
                        .build();
                SCMessageOuterClass.SCMessage message = builder.build();

                SpatialConnect.getInstance()
                        .getBackendService().publish("/store/tracking",message, QoS.AT_MOST_ONCE);
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
