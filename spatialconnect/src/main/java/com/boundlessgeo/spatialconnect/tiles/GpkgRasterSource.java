package com.boundlessgeo.spatialconnect.tiles;

import com.boundlessgeo.spatialconnect.geometries.SCPolygon;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCRasterStore;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.util.List;
import java.util.Map;

public class GpkgRasterSource implements SCRasterStore {

    private GeoPackageStore gpkgStore;

    public GpkgRasterSource(GeoPackageStore store) {
        this.gpkgStore = store;
    }

    @Override
    public TileOverlay overlayFromLayer(String layer, GoogleMap map) {
        Map<String, SCGpkgTileSource> tileSources = gpkgStore.getTileSources();
        if (tileSources.size() > 0 && tileSources.keySet().contains(layer)) {
            return map.addTileOverlay(
                    new TileOverlayOptions().tileProvider(
                            new GpkgTileProvider(tileSources.get(layer))
                    )
            );
        }
        return null;
    }

    @Override
    public SCPolygon getCoverage() {
        return null;
    }

    @Override
    public List<String> rasterLayers() {
        return gpkgStore.rasterLayers();
    }
}
