package com.boundlessgeo.spatialconnect.tiles;

import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;
import com.boundlessgeo.spatialconnect.geometries.SCPolygon;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCRasterStore;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GpkgRasterSource implements SCRasterStore {

    private GeoPackageStore gpkgStore;

    public GpkgRasterSource(GeoPackageStore store) {
        this.gpkgStore = store;
    }

    @Override
    public void overlayFromLayer(String layerName, GoogleMap map) {
        Map<String, SCGpkgTileSource> tileSources = ((GeoPackageAdapter) gpkgStore.getAdapter()).getTileSources();
        if (tileSources.size() > 0) {
            if (tileSources.keySet().contains(layerName)) {
                 map.addTileOverlay(
                         new TileOverlayOptions().tileProvider(
                                 new GpkgTileProvider(tileSources.get(layerName))
                         )
                 );
            }
        }
    }

    @Override
    public SCPolygon getCoverage() {
        return null;
    }

    @Override
    public List<SCGpkgTileSource> rasterList() {
        return new ArrayList<SCGpkgTileSource>(((GeoPackageAdapter)gpkgStore.getAdapter()).getTileSources().values());
    }
}
