package com.boundlessgeo.spatialconnect.db;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.boundlessgeo.spatialconnect.tiles.SCGpkgTileSource;
import com.boundlessgeo.spatialconnect.tiles.SCTileMatrixRow;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.QueryObservable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sqlite.database.SQLException;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Represents a single GeoPackage and provides methods for interacting with its features and tiles.
 */
public class GeoPackage {

  /**
   * The log tag for this class.
   */
  private final String LOG_TAG = GeoPackage.class.getSimpleName();

  /**
   * The name of the GeoPackage file.
   */
  private String name;

  /**
   * The instance of the database.
   */
  private BriteDatabase db;

  /**
   * The application context.
   */
  private Context context;

  /**
   * Determines if this GeoPackage passes
   */
  private boolean isValid = false;

  /**
   * A set of the {@link GeoPackageContents}
   */
  private HashSet<GeoPackageContents> contents = new HashSet<>();

  /**
   * A map of the {@link SCGpkgFeatureSource} which represent vector feature tables in this
   * GeoPackage.  The key is
   * the name of the table, which is unique in a GeoPackage.
   */
  private HashMap<String, SCGpkgFeatureSource> featureSources = new HashMap();

  /**
   * A map of the {@link SCGpkgTileSource}s in this GeoPackage where the key is the name of the
   * <a href="http://www.geopackage.org/spec/#tiles_user_tables">tile pyramid user data table</a>.
   */
  private HashMap<String, SCGpkgTileSource> tileSources = new HashMap();

  /**
   * Creates an instance of a {@link GeoPackage}. After creating a {@link BriteDatabase} for the
   * GeoPackage, it will
   * validate the db schema against the GeoPackage spec, then initialize the feature tables for use
   * in SpatialConnect.
   *
   * @param context the application context
   * @param name the name of the GeoPackage file
   */
  public GeoPackage(Context context, String name) {
    Log.d(LOG_TAG, "Initializing GeoPackage for " + name);
    this.name = name;
    this.context = context;
    try {
      db = new SCSqliteHelper(context, name).db();
      if (initializeSpatialMetadata() && validateGeoPackageSchema()) {
        initializeFeatureSources();
        getTileSources();
        isValid = true;
      }
    } catch (Exception ex) {
      Log.w(LOG_TAG, "Could not initialize GeoPackage b/c of " + ex.toString());
      isValid = false;
    }
  }

  private boolean initializeSpatialMetadata() {
    Log.d(LOG_TAG, "Initializing GeoPackage schema.");
    Cursor cursor = null;
    try {
      cursor = db.query("SELECT InitSpatialMetadata()");
      cursor.moveToFirst();
      return true;
    } catch (Exception ex) {
      Log.w(LOG_TAG,
          String.format("GeoPackage %s could not be initialized b/c %s", name, ex.toString()));
      return false;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  private boolean validateGeoPackageSchema() {
    Log.d(LOG_TAG, "Validating GeoPackage schema against the spec.");
    Cursor cursor = null;
    try {
      cursor = db.query("SELECT CheckSpatialMetadata()");
      cursor.moveToFirst();
      return true;
    } catch (Exception ex) {
      Log.w(LOG_TAG,
          String.format("GeoPackage %s could not be validated b/c %s", name, ex.toString()));
      return false;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * This method will check each feature table and ensure that r_tree indexes are setup (populated
   * and configured to
   * update with triggers) or create them if needed.
   */
  private void initializeFeatureSources() {
    for (SCGpkgFeatureSource source : getFeatureSources().values()) {
      // check if the spatial index tables exist for this feature table
      Cursor cursor = db.query("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?",
          new String[] { "rtree_" + source.getTableName() + "_" + source.getGeomColumnName() });
      try {
        if (cursor.moveToFirst()) {
          if (cursor.getInt(0) == 0) {
            cursor.close();  // close the cursor before executing the query to create the index
            createSpatialIndex(source.getTableName(), source.getGeomColumnName(),
                source.getPrimaryKeyName());
          }
        }
      } finally {
        cursor.close();
      }
    }
  }

  // executes the CreateSpatialIndex function for a given table
  private void createSpatialIndex(String tableName, String geomColumnName, String pkName) {
    Log.d(LOG_TAG, "Creating index for table " + tableName);
    Cursor cursor = null;
    try {
      cursor = db.query(
          String.format("SELECT CreateSpatialIndex('%s', '%s', '%s')", tableName, geomColumnName,
              pkName));
      cursor.moveToFirst(); // force query to execute
    } catch (SQLException ex) {
      ex.printStackTrace();
      Log.w(LOG_TAG, "Could not create index b/c " + ex.getMessage());
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * Returns a {@link GeoPackageContents} for each row in the gpkg_contents table.
   *
   * @return observable stream of the list of {@link GeoPackageContents}
   */
  public Observable<List<GeoPackageContents>> getGpkgContents() {
    return db.createQuery("gpkg_contents", "SELECT * FROM gpkg_contents")
        .mapToList(new Func1<Cursor, GeoPackageContents>() {
          @Override public GeoPackageContents call(Cursor cursor) {
            return createGeoPackageContents(cursor);
          }
        })
        .doOnError(new Action1<Throwable>() {
          @Override public void call(Throwable throwable) {
            Log.e(LOG_TAG, String.format("Error reading gpkg_contents table in database %s!", name),
                throwable);
            Log.d(LOG_TAG, String.format("The size of the %s db file is %d", name,
                context.getDatabasePath(name).length()));
          }
        });
  }

  // returns an array of int values where the first is the min and the second is the max zoom level for the table
  public int[] getMinMax(String tableName) {
    Log.d(LOG_TAG, "Retrieving min/max zoom levels for table " + tableName);
    Cursor cursor = null;
    int[] minMax = new int[2];
    try {
      cursor = db.query(
          String.format("SELECT MIN(zoom_level) AS min, MAX(zoom_level) AS max FROM '%s'",
              tableName));
      if (cursor.moveToFirst()) {
        minMax[0] = cursor.getInt(0);
        minMax[1] = cursor.getInt(1);
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
      Log.w(LOG_TAG, "Could not retrieving min/max zoom levels b/c " + ex.getMessage());
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return minMax;
  }

  // gets the organization_coordsys_id from the gpkg_spatial_ref_sys table (eg 4326 or 3857)
  public Integer getOrganizationCoordSysId(Integer srsId) {
    Cursor cursor = null;
    Integer coordSysId = -1;
    try {
      cursor = db.query(String.format(
          "SELECT organization_coordsys_id FROM gpkg_spatial_ref_sys WHERE srs_id = %d", srsId));
      if (cursor.moveToFirst()) {
        coordSysId = cursor.getInt(0);
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
      Log.w(LOG_TAG, "Could not retrieve organization_coordsys_id b/c " + ex.getMessage());
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return coordSysId;
  }

  public HashMap<Integer, SCTileMatrixRow> getTileRowMatrix(String tableName) {
    Log.d(LOG_TAG, "Building tile matrix for table " + tableName);
    Cursor cursor = null;
    HashMap<Integer, SCTileMatrixRow> matrix = new HashMap<>();
    try {
      cursor = db.query(
          String.format("SELECT * FROM gpkg_tile_matrix WHERE table_name = '%s'", tableName));
      while (cursor.moveToNext()) {
        int zoomLevel = SCSqliteHelper.getInt(cursor, "zoom_level");
        matrix.put(zoomLevel,
            new SCTileMatrixRow(tableName, zoomLevel, SCSqliteHelper.getInt(cursor, "matrix_width"),
                SCSqliteHelper.getInt(cursor, "matrix_height"),
                SCSqliteHelper.getInt(cursor, "tile_width"),
                SCSqliteHelper.getInt(cursor, "tile_height"),
                SCSqliteHelper.getDouble(cursor, "pixel_x_size"),
                SCSqliteHelper.getDouble(cursor, "pixel_y_size")));
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
      Log.w(LOG_TAG, "Could not build tile matrix b/c " + ex.getMessage());
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return matrix;
  }

  // build a GeoPackageContents object from a Cursor instance
  private GeoPackageContents createGeoPackageContents(Cursor cursor) {
    return new GeoPackageContents(SCSqliteHelper.getString(cursor, "table_name"),
        GeoPackageContents.DataType.valueOf(
            SCSqliteHelper.getString(cursor, "data_type").toUpperCase()),
        SCSqliteHelper.getString(cursor, "identifier"),
        SCSqliteHelper.getString(cursor, "description"),
        SCSqliteHelper.getString(cursor, "last_change"),
        Double.valueOf(SCSqliteHelper.getLong(cursor, "min_x")),
        Double.valueOf(SCSqliteHelper.getLong(cursor, "min_y")),
        Double.valueOf(SCSqliteHelper.getLong(cursor, "max_x")),
        Double.valueOf(SCSqliteHelper.getLong(cursor, "max_y")),
        SCSqliteHelper.getInt(cursor, "srs_id"));
  }

  /**
   * Returns an observable of a List of {@link SCGpkgFeatureSource}s, one for each feature table in
   * the GeoPackage.
   *
   * @return observable stream of the list of feature tables as {@link SCGpkgFeatureSource}s
   */
  public Observable<List<SCGpkgFeatureSource>> getFeatureTables() {
    return Observable.from(getGeoPackageContents())
        .filter(new Func1<GeoPackageContents, Boolean>() {
          @Override public Boolean call(GeoPackageContents geoPackageContents) {
            return geoPackageContents.getTableType().equals(GeoPackageContents.DataType.FEATURES);
          }
        })
        .flatMap(new Func1<GeoPackageContents, Observable<SCGpkgFeatureSource>>() {
          @Override
          public Observable<SCGpkgFeatureSource> call(GeoPackageContents geoPackageContents) {
            String tableName = geoPackageContents.getTableName();
            // execute a query to get schema for this feature table
            Cursor cursor = db.query(
                // need to use String.format b/c you can't prepare PRAGMA queries:
                // http://stackoverflow.com/questions/2389813/cant-prepare-pragma-queries-on-android
                String.format("PRAGMA table_info(%s)", tableName));
            if (cursor == null) {
              Log.e(LOG_TAG, "Something wrong with the PRAGMA table_info query.");
              return Observable.empty();
            }
            // build a feature source from the table schema
            SCGpkgFeatureSource source = createFeatureSource(tableName);
            while (cursor.moveToNext()) {
              String columnName = SCSqliteHelper.getString(cursor, "name");
              if (SCSqliteHelper.getString(cursor, "type").equalsIgnoreCase("GEOMETRY")
                  || SCSqliteHelper.getString(cursor, "type").equalsIgnoreCase("POINT")
                  || SCSqliteHelper.getString(cursor, "type").equalsIgnoreCase("LINESTRING")
                  || SCSqliteHelper.getString(cursor, "type").equalsIgnoreCase("POLYGON")) {
                source.setGeomColumnName(columnName);
              } else if (SCSqliteHelper.getInt(cursor, "pk") == 1) {
                source.setPrimaryKeyName(columnName);
              } else {
                source.addColumn(columnName, SCSqliteHelper.getString(cursor, "type"));
              }
            }
            return Observable.just(source);
          }
        })
        .toList();
  }

  public Observable<List<SCGpkgTileSource>> getTileTables() {
    return Observable.from(getGeoPackageContents())
        .filter(new Func1<GeoPackageContents, Boolean>() {
          @Override public Boolean call(GeoPackageContents geoPackageContents) {
            return geoPackageContents.getTableType().equals(GeoPackageContents.DataType.TILES);
          }
        })
        .flatMap(new Func1<GeoPackageContents, Observable<SCGpkgTileSource>>() {
          @Override
          public Observable<SCGpkgTileSource> call(GeoPackageContents geoPackageContents) {
            SCGpkgTileSource source = createTileSource(geoPackageContents);
            return Observable.just(source);
          }
        })
        .toList();
  }

  // helper method to create a SCGpkgFeatureSource for this GeoPackage.
  private SCGpkgFeatureSource createFeatureSource(String tableName) {
    return new SCGpkgFeatureSource(this, tableName);
  }

  private SCGpkgTileSource createTileSource(GeoPackageContents contents) {
    return new SCGpkgTileSource(this, contents);
  }

  public Set<GeoPackageContents> getGeoPackageContents() {
    if (this.contents.size() == 0) {
      refreshGeoPackageContents();
    }
    return this.contents;
  }

  public Map<String, SCGpkgFeatureSource> getFeatureSources() {
    if (this.featureSources.size() == 0) {
      refreshFeatureSources();
    }
    return this.featureSources;
  }

  public Map<String, SCGpkgTileSource> getTileSources() {
    if (this.tileSources.size() == 0) {
      List<SCGpkgTileSource> tileSources = getTileTables().toBlocking().first();
      for (SCGpkgTileSource source : tileSources) {
        this.tileSources.put(source.getTableName(), source);
      }
    }
    return this.tileSources;
  }

  public void refreshGeoPackageContents() {
    List<GeoPackageContents> contents = getGpkgContents().toBlocking().first();
    this.contents = new HashSet<>(contents);
  }

  public void refreshFeatureSources() {
    refreshGeoPackageContents();
    List<SCGpkgFeatureSource> featureTables = getFeatureTables().toBlocking().first();
    for (SCGpkgFeatureSource source : featureTables) {
      this.featureSources.put(source.getTableName(), source);
    }
  }

  public SCGpkgFeatureSource getFeatureSourceByName(String name) {
    return this.featureSources.get(name);
  }

  public Set<String> getFeatureSourceNames() {
    return this.featureSources.keySet();
  }

  /**
   * Close the db connection.
   */
  public void close() {
    //TODO: unsubscribe all subscribers to querys on this db
    db.close();
  }

  /**
   * Calls createQuery on the {@link BriteDatabase} instance for this GeoPackage.
   */
  public QueryObservable createQuery(String table, String sql, String... args) {
    return db.createQuery(table, sql, args);
  }

  /**
   * Calls executeAndTrigger on the {@link BriteDatabase} instance for this GeoPackage.
   */
  public void executeAndTrigger(String table, String sql) {
    db.executeAndTrigger(table, sql);
  }

  /**
   * Calls query on the {@link BriteDatabase} instance for this GeoPackage.
   */
  public Cursor query(String sql, String... args) {
    return db.query(sql, args);
  }

  public BriteDatabase.Transaction newTransaction() {
    return db.newTransaction();
  }

  public void removeFromGpkgContents(String tableName) {
    db.delete("gpkg_contents", "table_name=?", new String[] { tableName });
  }

  public void removeFromGpkgGeometryColumns(String tableName) {
    db.delete("gpkg_geometry_columns", "table_name=?", new String[] { tableName });
  }

  public String getName() {
    return name;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GeoPackage that = (GeoPackage) o;

    return name.equals(that.name);
  }

  public boolean isValid() {
    return isValid;
  }

  @Override public int hashCode() {
    return name.hashCode();
  }
}
