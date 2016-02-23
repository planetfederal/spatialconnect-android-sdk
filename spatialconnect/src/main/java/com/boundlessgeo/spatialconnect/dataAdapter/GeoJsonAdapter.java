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
package com.boundlessgeo.spatialconnect.dataAdapter;


import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryCollection;
import com.boundlessgeo.spatialconnect.geometries.SCGeometryFactory;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

import rx.Observable;
import rx.functions.Func1;


public class GeoJsonAdapter extends SCDataAdapter {
    private static final String NAME = "GeoJsonAdapter";
    private static final String TYPE = "GeoJson";
    private static final int VERSION = 1;
    private static final String DEFAULTLAYER = "d";
    private Context context;
    private static final String LOG_TAG = GeoPackageAdapter.class.getSimpleName();

    public GeoJsonAdapter(Context context, SCStoreConfig scStoreConfig) {
        super(NAME, TYPE, VERSION);
        this.context = context;
        this.scStoreConfig = scStoreConfig;
    }

    public Observable connect() {
        super.connect();

        // if the file was packaged with the application, then attempt to connect
        if (scStoreConfig.isMainBundle()) {
            // first check if the GeoJson file already exists in the internal storage location specified in the uri
            File f = new File(context.getFilesDir(), scStoreConfig.getUri());
            if (!f.exists()) {
                InputStream is = null;
                // if the doesn't exist, then we attempt to create it by copying it from the raw
                // resources to the destination specified in the config
                try {
                    int resourceId = context.getResources().getIdentifier(
                            scStoreConfig.getUri().split("\\.")[0], "raw", context.getPackageName()
                    );
                    is = context.getResources().openRawResource(resourceId);
                    FileUtils.copyInputStreamToFile(is, f);
                    super.connected();
                } catch (IOException e) {
                    Log.w(LOG_TAG, "Couldn't connect to geojson store.", e);
                    this.disconnect();
                    e.printStackTrace();
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Couldn't close the stream.", e);
                        }
                    }
                }
            } else { // the file already exists so set the status to connected
                super.connected();
                return null;
            }
        }
        // TODO: else, look for the geojson file in other locations
        return null;
    }

    public void disconnect() {
        super.disconnect();
    }

    // using a GeoJSON file to queryFeature is not going to be fast
    public Observable<SCSpatialFeature> query(final SCQueryFilter filter) {
        HashSet<SCSpatialFeature> scSpatialFeatures = new HashSet<SCSpatialFeature>();
        SCGeometryFactory factory = new SCGeometryFactory();
        // TODO: updated the Json --> SCSpatialFetures code to use InputStreams instead of Strings
        final SCGeometryCollection collection = factory.getGeometryCollectionFromFeatureCollectionJson(
                getResourceAsString()
        );

        return Observable.from(collection.getFeatures())
          .filter(
            new Func1<SCSpatialFeature, Boolean>() {
              @Override
              public Boolean call(SCSpatialFeature feature) {
                if (feature instanceof SCGeometry &&
                  ((SCGeometry) feature).getGeometry() != null &&
                  filter.getPredicate().applyFilter((SCGeometry) feature)) {
                  return true;
                } else {
                  return false;
                }
              }
            }
          ).map(new Func1<SCSpatialFeature, SCSpatialFeature>() {
            @Override
            public SCSpatialFeature call(SCSpatialFeature scSpatialFeature) {
              scSpatialFeature.setStoreId(scStoreConfig.getUniqueID());
              scSpatialFeature.setLayerId(DEFAULTLAYER);
              scSpatialFeature.setId(scSpatialFeature.getId());
              return scSpatialFeature;
            }
          });

    }


    public void create(SCSpatialFeature spatialFeatureToAdd) {

    }

    public void update(SCSpatialFeature scSpatialFeature) {

    }

    public void delete(String featureId) {

    }


    private InputStream getResourceStream() {
        int resourceId = context.getResources().getIdentifier(
                scStoreConfig.getUri().split("\\.")[0], "raw", context.getPackageName()
        );
        return context.getResources().openRawResource(resourceId);
    }

    /**
     * Returns a String of the GeoJson file associated with the adapter's data store.
     *
     * @return
     */
    private String getResourceAsString() {
        int resourceId = context.getResources().getIdentifier(
                scStoreConfig.getUri().split("\\.")[0], "raw", context.getPackageName()
        );
        InputStream is = context.getResources().openRawResource(resourceId);
        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            while ((line = bufferedreader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Couldn't read the stream.", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Couldn't close the stream.", e);
            }
        }
        return stringBuilder.toString();
    }

}



