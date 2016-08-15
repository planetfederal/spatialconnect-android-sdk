package com.boundlessgeo.spatialconnect.db;

/**
 * POJO for <a href="http://www.geopackage.org/spec/#_contents">gpkg_contents</a> table.
 */
public class GeoPackageContents {

    private String tableName;
    private DataType tableType;
    private String identifier;
    private String description;
    private String lastChange; // timestamp value in ISO 8601 format
    private Double minX;
    private Double minY;
    private Double maxX;
    private Double maxY;
    private Integer srsId;

    public GeoPackageContents(String tableName, DataType tableType, String identifier, String description, String lastChange, Double minX, Double minY, Double maxX, Double maxY, Integer srsId) {
        this.tableName = tableName;
        this.tableType = tableType;
        this.identifier = identifier;
        this.description = description;
        this.lastChange = lastChange;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.srsId = srsId;
    }

    public enum DataType {
        FEATURES, TILES
    }

    public DataType getTableType() {
        return tableType;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        return "GeoPackageContents{" +
                "tableName='" + tableName + '\'' +
                ", tableType=" + tableType +
                ", identifier='" + identifier + '\'' +
                ", description='" + description + '\'' +
                ", lastChange='" + lastChange + '\'' +
                ", minX=" + minX +
                ", minY=" + minY +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                ", srsId=" + srsId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoPackageContents that = (GeoPackageContents) o;

        return tableName.equals(that.tableName);

    }

    @Override
    public int hashCode() {
        return tableName.hashCode();
    }
}
