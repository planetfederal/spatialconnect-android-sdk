package com.boundlessgeo.spatialconnect.stores;

import com.boundlessgeo.spatialconnect.geometries.SCPolygon;
import com.boundlessgeo.spatialconnect.tiles.SCGpkgTileSource;
import com.google.android.gms.maps.GoogleMap;
import java.util.List;

public interface SCRasterStore {

  void overlayFromLayer(String layerName, GoogleMap map);

  SCPolygon getCoverage();

  List<SCGpkgTileSource> rasterList();
}
