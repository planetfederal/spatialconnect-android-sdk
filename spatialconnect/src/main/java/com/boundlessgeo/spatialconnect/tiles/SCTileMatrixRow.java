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

public class SCTileMatrixRow {

  private String tableName;
  private Integer zoomLevel;
  private Integer matrixWidth;
  private Integer matrixHeight;
  private Integer tileWidth;
  private Integer tileHeight;
  private Double pixelXSize;
  private Double pixelYSize;

  public SCTileMatrixRow(String tableName, Integer zoomLevel, Integer matrixWidth,
      Integer matrixHeight, Integer tileWidth, Integer tileHeight, Double pixelXSize,
      Double pixelYSize) {
    this.tableName = tableName;
    this.zoomLevel = zoomLevel;
    this.matrixWidth = matrixWidth;
    this.matrixHeight = matrixHeight;
    this.tileWidth = tileWidth;
    this.tileHeight = tileHeight;
    this.pixelXSize = pixelXSize;
    this.pixelYSize = pixelYSize;
  }

  public String getTableName() {
    return tableName;
  }

  public Integer getZoomLevel() {
    return zoomLevel;
  }

  public Integer getMatrixWidth() {
    return matrixWidth;
  }

  public Integer getMatrixHeight() {
    return matrixHeight;
  }

  public Integer getTileWidth() {
    return tileWidth;
  }

  public Integer getTileHeight() {
    return tileHeight;
  }

  public Double getPixelXSize() {
    return pixelXSize;
  }

  public Double getPixelYSize() {
    return pixelYSize;
  }
}
