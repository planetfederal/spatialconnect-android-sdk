package com.boundlessgeo.spatialconnect.services;

import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.R;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.config.Stores;
import com.boundlessgeo.spatialconnect.scutilities.Json.ObjectMappers;
import com.boundlessgeo.spatialconnect.stores.GeoJsonStore;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * When instantiated, the SCServiceManager adds the default services and registers all stores
 * defined in the config file.  The SCServiceManager is also used to manage the services (starting,
 * stopping, creating, deleting, etc).
 */
public class SCServiceManager {
    private Map<String, SCService> services;
    private SCDataService dataService;
    private SCNetworkService networkService;
    private SCSensorService sensorService;
    private Context context;
    private final String LOG_TAG = SCServiceManager.class.getSimpleName();

    public SCServiceManager(Context context) {
        this.services = new HashMap<>();
        this.dataService = new SCDataService();
        this.networkService = new SCNetworkService();
        this.sensorService = new SCSensorService(context);
        this.context = context;
        addDefaultServices();
        initializeDataStores(this.context.getResources().openRawResource(R.raw.scconfig));
    }

    public void addDefaultServices() {
        addService(this.dataService);
        addService(this.networkService);
        addService(this.sensorService);
    }

    /**
     * Reads the static config file packaged with the APK to determine what data stores are defined,
     * then register each supported data store with the dataService.
     *
     * @param inputStream instance of the inputstream of the SpatialConnect config file
     */
    public void initializeDataStores(InputStream inputStream) {
        Log.i(LOG_TAG, "Initializing data stores.");
        try {
            final ObjectMapper mapper = ObjectMappers.getMapper();
            final Stores stores = mapper.readValue(inputStream, Stores.class);
            final List<SCStoreConfig> scStoreConfigs = stores.getStores();
            for (SCStoreConfig scStoreConfig : scStoreConfigs) {
                String key =
                        scStoreConfig.getType() + "." + scStoreConfig.getVersion();
                Log.d(LOG_TAG, "Attempting to register " + key);
                if (dataService.isStoreSupported(key)) {
                    if (key.equals("geojson.1")) {
                        dataService.registerStore(
                                new GeoJsonStore(context, scStoreConfig)
                        );
                        Log.d(LOG_TAG, "Registered geojson.1 store.");
                    } else if (key.equals("geopackage.1")) {
                        dataService.registerStore(
                                new GeoPackageStore(context, scStoreConfig)
                        );
                        Log.d(LOG_TAG, "Registered geopackage.1 store.");
                    }
                }
            }
        } catch (Exception ex) {
            //TODO: test this with a bad config file.
            Log.e(LOG_TAG, "Error loading configuration file", ex);
            System.exit(0);
        }
    }

    public void addService(SCService service) {
        this.services.put(service.getId(), service);
    }

    public void removeService(SCService service) {
        this.services.remove(service.getId());
    }

    public void startService(String id) {
        this.services.get(id).start();
    }

    public void stopService(String id) {
        this.services.get(id).stop();
    }

    public void restartService(String id) {
        this.services.get(id).stop();
        this.services.get(id).start();
    }

    public void startAllServices() {
        for (String key : this.services.keySet()) {
            this.services.get(key).start();
        }
    }

    public void stopAllServices() {
        for (String key : this.services.keySet()) {
            this.services.get(key).stop();
        }
    }

    public void restartAllServices() {
        for (String key : this.services.keySet()) {
            this.services.get(key).stop();
            this.services.get(key).start();
        }
    }


    //region Getters
    public Map<String, SCService> getServices() {
        return services;
    }

    public SCDataService getDataService() {
        return dataService;
    }

    public SCNetworkService getNetworkService() {
        return networkService;
    }

    public SCSensorService getSensorService() {
        return sensorService;
    }

    public Context getContext() {
        return context;
    }

    //endregion
}

