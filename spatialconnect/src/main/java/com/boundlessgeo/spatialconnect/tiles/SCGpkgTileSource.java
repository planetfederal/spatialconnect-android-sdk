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

import android.database.Cursor;
import android.util.Log;
import com.boundlessgeo.spatialconnect.db.GeoPackage;
import com.boundlessgeo.spatialconnect.db.GeoPackageContents;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import java.util.HashMap;
import java.util.Map;
import org.sqlite.database.SQLException;

/**
 * This class is responsible for reading tile data from a GeoPackage.  It stores information from
 * the gpkg_tile_matrix_set and gpkg_tile_matrix tables and provides methods for retrieving the tile images.
 *
 * @see <a href="http://www.geopackage.org/spec/#tiles">http://www.geopackage.org/spec/#tiles</a>
 */
public class SCGpkgTileSource {

  private static final double MERCATOR_OFFSET = 20037508.342789244;

  /**
   * The log tag for this class.
   */
  private final String LOG_TAG = SCGpkgTileSource.class.getSimpleName();

  /**
   * The instance of the GeoPackage used to connect to the database.
   */
  private GeoPackage gpkg;

  /**
   * The name of the table.
   */
  private String tableName;

  /**
   * The integer of the spatial reference system used for the tiles (ex 4326).
   */
  private Integer srsId;

  /**
   * The minimum zoom level of the tile pyramid.
   */
  private Integer minZoom;

  /**
   * The maximum zoom level of the tile pyramid.
   */
  private Integer maxZoom;

  /**
   * The minimum bounding box for all tiles in the tile pyramid.
   */
  private SCBoundingBox bbox;

  /**
   * Map of the tile matrix rows where the keys are zoom levels.
   */
  private HashMap<Integer, SCTileMatrixRow> matrix;

  /**
   * Creates and instance of the {@link SCGpkgTileSource} using the {@link GeoPackage} containing the db
   * connection.
   *
   * @param geoPackage
   * @param contents
   */
  public SCGpkgTileSource(GeoPackage geoPackage, GeoPackageContents contents) {
    this.gpkg = geoPackage;
    this.tableName = contents.getTableName();
    this.srsId = gpkg.getOrganizationCoordSysId(contents.getSrsId());
    this.bbox = new SCBoundingBox(contents.getMinX(), contents.getMinY(), contents.getMaxX(),
        contents.getMaxY());
    int[] minMax = gpkg.getMinMax(contents.getTableName());
    this.minZoom = minMax[0];
    this.maxZoom = minMax[1];
    this.matrix = gpkg.getTileRowMatrix(contents.getTableName());
  }

  public String getTableName() {
    return tableName;
  }

  public Integer getSrsId() {
    return srsId;
  }

  public Integer getMinZoom() {
    return minZoom;
  }

  public Integer getMaxZoom() {
    return maxZoom;
  }

  public SCBoundingBox getBbox() {
    return bbox;
  }

  public HashMap<Integer, SCTileMatrixRow> getMatrix() {
    return matrix;
  }

  /**
   * Retrieves a tile from the GeoPackage.
   *
   * @param x
   * @param y
   * @param zoom
   * @return the {@link Tile} for the given x, y and zoom level
   */
  public Tile getTile(int x, int y, int zoom) {
    Cursor cursor = null;
    Tile tile = TileProvider.NO_TILE;
    int[] gpkgXY = getGpkgXY(x, y, zoom);
    SCTileMatrixRow rowForZoom = matrix.get(Integer.valueOf(zoom));
    if (gpkgXY != null) {
      try {
        String queryString = String.format(
            "SELECT tile_data FROM %s WHERE zoom_level = %d AND tile_column = %d AND tile_row = %d",
            tableName, zoom, gpkgXY[0], gpkgXY[1]);
        cursor = gpkg.query(queryString);
        if (cursor.moveToFirst()) {
          tile = new Tile(rowForZoom.getTileWidth(), rowForZoom.getTileHeight(), cursor.getBlob(0));
        }
      } catch (SQLException ex) {
        ex.printStackTrace();
        Log.w(LOG_TAG, "Could not tile data b/c " + ex.getMessage());
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
    }
    return tile;
  }

  private int[] getGpkgXY(int x, int y, int zoom) {
    int tilesPerSide = (int) Math.pow(2, zoom);
    double tileSizeMeters = (2 * MERCATOR_OFFSET) / tilesPerSide;
    double tileMinX = (-1 * MERCATOR_OFFSET) + (x * tileSizeMeters);
    //        double tileMaxX = (-1 * MERCATOR_OFFSET) + ((x + 1) * tileSizeMeters);
    //        double tileMinY = MERCATOR_OFFSET - ((y + 1) * tileSizeMeters);
    double tileMaxY = MERCATOR_OFFSET - (y * tileSizeMeters);

    SCTileMatrixRow rowForZoom = matrix.get(Integer.valueOf(zoom));
    double metersPerPixelX = tileSizeMeters / rowForZoom.getTileWidth();

    double minDelta = MERCATOR_OFFSET * 2;
    int zoomRowMatch = -1;
    for (Map.Entry<Integer, SCTileMatrixRow> e : matrix.entrySet()) {
      double diff = Math.abs(metersPerPixelX - e.getValue().getPixelXSize());
      if (diff < minDelta) {
        minDelta = diff;
        zoomRowMatch = e.getKey();
      }
    }

    if (zoomRowMatch == -1) {
      return null;
    }

    double coverageUpperLeftXMeters = bbox.getMinX();
    double coverageUpperLeftYMeters = bbox.getMaxY();
    double coverageInsetXMeters = tileMinX - coverageUpperLeftXMeters;
    double coverageInsetYMeters = coverageUpperLeftYMeters - tileMaxY;

    if (coverageInsetXMeters < 0 || coverageInsetYMeters < 0) {
      return null;
    }

    int tileRetrieveX = (int) Math.round(coverageInsetXMeters / tileSizeMeters);
    int tileRetrieveY = (int) Math.round(coverageInsetYMeters / tileSizeMeters);

    return new int[] { tileRetrieveX, tileRetrieveY };
  }
}