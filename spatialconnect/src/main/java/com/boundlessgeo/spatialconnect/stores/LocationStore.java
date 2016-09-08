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

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.geometries.SCPoint;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.schema.SCMessageOuterClass;

import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.functions.Action0;

public class LocationStore extends GeoPackageStore {

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
                typeDefs.put("accuracy","TEXT");
                typeDefs.put("timestamp","INTEGER");

                addLayer("last_known_location", typeDefs);
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
                SCMessageOuterClass.SCMessage.Builder builder =  SCMessageOuterClass.SCMessage.newBuilder();
                builder.setAction(100)
                        .setPayload(point.toJson())
                        .build();
                SCMessageOuterClass.SCMessage message = builder.build();

                SpatialConnect.getInstance().getBackendService().publishReplyTo("/store/tracking",message);
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
