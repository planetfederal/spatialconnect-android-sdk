/*
 *
 *  * ****************************************************************************
 *  *  Licensed to the Apache Software Foundation (ASF) under one
 *  *  or more contributor license agreements.  See the NOTICE file
 *  *  distributed with this work for additional information
 *  *  regarding copyright ownership.  The ASF licenses this file
 *  *  to you under the Apache License, Version 2.0 (the
 *  *  "License"); you may not use this file except in compliance
 *  *  with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing,
 *  *  software distributed under the License is distributed on an
 *  *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  *  KIND, either express or implied.  See the License for the
 *  *  specific language governing permissions and limitations
 *  *  under the License.
 *  * ****************************************************************************
 *
 */

package com.boundlessgeo.spatialconnect.dataAdapter;


import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;

import java.net.MalformedURLException;
import java.net.URL;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageException;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;

/**
 * This adpater connects to a specific geopackage defined in the scStoreConfig and provides an
 * instance of GeoPackageManager used to interact with the GeoPackages.
 */
public class GeoPackageAdapter extends SCDataAdapter {

    private static final String NAME = "GeoPackageAdapter";
    private static final String TYPE = "GeoPackage";
    private static final int VERSION = 1;
    private Context context;
    private final String LOG_TAG = GeoPackageAdapter.class.getSimpleName();

    /**
     * GeoPackage manager
     */
    private GeoPackageManager manager;

    public GeoPackageAdapter(Context context, SCStoreConfig scStoreConfig) {
        super(NAME, TYPE, VERSION);
        this.context = context;
        this.scStoreConfig = scStoreConfig;
        // initialize geopackage manager
        manager = GeoPackageFactory.getManager(context);
    }

    @Override
    public void connect() {
        this.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTING);

        try {
            // TODO: we need to determine if the store's URI is local to the filesystem (packaged
            // with the app or on an SD card and try loading from there)
            URL theUrl = new URL(scStoreConfig.getUri());

            // download geopackage and store it locally if it's not present
            if (!manager.exists(scStoreConfig.getName())) {
                Log.d(LOG_TAG, "Attempting to download geopackage from " + theUrl);

                if (!manager.importGeoPackage(scStoreConfig.getName(), theUrl)) {
                    Log.w(LOG_TAG, "Failed to import GeoPackage from " + theUrl);
                    Log.w(LOG_TAG, "Could not connect to GeoPackage");
                    this.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
                } else {
                    Log.d(LOG_TAG, "Successfully downloaded geopackage from " + theUrl);
                }
            } else {
                Log.d(LOG_TAG, "GeoPackage " + scStoreConfig.getName() + " already downloaded.");
            }

            // connect to the geopackage on the local filesystem
            GeoPackage geoPackage = manager.open(scStoreConfig.getName());

            if (geoPackage != null) {
                this.setStatus(SCDataAdapterStatus.DATA_ADAPTER_CONNECTED);
            } else {
                this.setStatus(SCDataAdapterStatus.DATA_ADAPTER_DISCONNECTED);
            }
        } catch (MalformedURLException | GeoPackageException ex) {
            Log.e(LOG_TAG, "Couldn't download the geopackage.", ex);
        }
    }

    public GeoPackageManager getGeoPackageManager() {
        return manager;
    }

}