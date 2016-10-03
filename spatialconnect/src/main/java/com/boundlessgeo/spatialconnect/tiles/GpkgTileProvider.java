/**
 * Copyright 2016 Boundless, http://boundlessgeo.com
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
 * See the License for the specific language governing permissions and limitations under the
 * License
 */
package com.boundlessgeo.spatialconnect.tiles;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

public class GpkgTileProvider implements TileProvider {

  private SCGpkgTileSource tileSource;

  public GpkgTileProvider(SCGpkgTileSource tileSource) {
    this.tileSource = tileSource;
  }

  @Override public Tile getTile(int x, int y, int zoom) {
    if (!tileExists(x, y, zoom)) {
      return NO_TILE;
    }
    return tileSource.getTile(x, y, zoom);
  }

  private boolean tileExists(int x, int y, int zoom) {
    if ((zoom < tileSource.getMinZoom() || zoom > tileSource.getMaxZoom())) {
      return false;
    }
    return true;
  }
}
