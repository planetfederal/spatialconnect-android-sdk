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

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.db.GeoPackage;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * This adpater connects to a specific GeoPackage defined by a {@link SCStoreConfig}.  The adapter will check to see if
 * the GeoPackage file already exists on disk and if not, it will attempt to download it.  After successfully
 * obtaining the file, it then creates a {@link GeoPackage} instance for interacting with that file.
 *
 * The adapter also provides the CRUD implementation to the {@link GeoPackageStore}.
 */
public class GeoPackageAdapter {

    private static final String NAME = "GeoPackageAdapter";
    private static final String TYPE = "gpkg";
    private static final int VERSION = 1;
    private Context context;
    private final String LOG_TAG = GeoPackageAdapter.class.getSimpleName();
    protected GeoPackage gpkg;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();


    public GeoPackageAdapter(Context context, SCStoreConfig scStoreConfig) {
        //super(NAME, TYPE, VERSION);
        this.context = context;
        //this.scStoreConfig = scStoreConfig;
    }



}
