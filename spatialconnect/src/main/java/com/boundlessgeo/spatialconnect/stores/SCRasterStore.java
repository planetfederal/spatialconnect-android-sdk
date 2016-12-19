package com.boundlessgeo.spatialconnect.stores;

import com.boundlessgeo.spatialconnect.geometries.SCPolygon;
import com.boundlessgeo.spatialconnect.tiles.SCGpkgTileSource;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.util.List;

public interface SCRasterStore {

    TileOverlay overlayFromLayer(String layerName, GoogleMap map);
    SCPolygon getCoverage();
    List<String> rasterLayers();
}
