package com.boundlessgeo.spatialconnect.services;

import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.config.Stores;
import com.boundlessgeo.spatialconnect.scutilities.Json.ObjectMappers;
import com.boundlessgeo.spatialconnect.scutilities.Storage.SCFileUtilities;
import com.boundlessgeo.spatialconnect.stores.GeoJsonStore;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;

import java.io.File;
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
    private static final String CONFIGS_DIR = "configs";

    public SCServiceManager(Context context) {
        this.services = new HashMap<>();
        this.dataService = new SCDataService();
        this.networkService = new SCNetworkService();
        this.sensorService = new SCSensorService(context);
        this.context = context;
        addDefaultServices();
        loadConfigs();
    }

    public void addDefaultServices() {
        addService(this.dataService);
        addService(this.networkService);
        addService(this.sensorService);
    }

    /**
     * Loads all config files and registers the stores in each config. The config files need to be valid JSON files
     * with an ".scfg" extension and must be packaged within the app's
     * <a href="http://developer.android.com/guide/topics/data/data-storage.html#filesExternal">external storage</a>
     * directory for config files.
     */
    public void loadConfigs() {
        Log.i(LOG_TAG,
                "Searching for config files in external storage directory " + context.getExternalFilesDir(CONFIGS_DIR)
        );
        File[] configFiles = SCFileUtilities.findFilesByExtension(context.getExternalFilesDir(CONFIGS_DIR), ".scfg");
        for (File file : configFiles) {
            Log.d(LOG_TAG, "Registering stores for config " + file.getName());
            registerStores(file);
        }
    }

    /**
     * Reads a config file to determine what stores are defined, then registers each supported data store with the
     * dataService.
     *
     * @param f - a valid SpatialConnect config file
     */
    protected void registerStores(File f) {
        try {
            final Stores stores = ObjectMappers.getMapper().readValue(f, Stores.class);
            final List<SCStoreConfig> scStoreConfigs = stores.getStores();
            for (SCStoreConfig scStoreConfig : scStoreConfigs) {
                String key = scStoreConfig.getType() + "." + scStoreConfig.getVersion();
                if (dataService.isStoreSupported(key)) {
                    if (key.equals("geojson.1")) {
                        dataService.registerStore(new GeoJsonStore(context, scStoreConfig));
                        Log.d(LOG_TAG, "Registered geojson.1 store " + scStoreConfig.getName());
                    }
                    else if (key.equals("gpkg.1")) {
                        dataService.registerStore(new GeoPackageStore(context, scStoreConfig));
                        Log.d(LOG_TAG, "Registered gpkg.1 store " + scStoreConfig.getName());
                    }
                }
            }
        } catch (Exception ex) {
            //TODO: test this with a bad config file.
            Log.w(LOG_TAG, "Couldn't register stores for " + f.getName(), ex);
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
}

