package com.boundlessgeo.spatialconnect.test;

import android.database.Cursor;

import com.boundlessgeo.spatialconnect.db.SCSqliteHelper;
import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.squareup.sqlbrite.BriteDatabase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SCSqliteHelperTest extends BaseTestCase {

    private static SpatialConnect sc;
    private static BriteDatabase rio;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(remoteConfigFile);
        sc.startAllServices();
        sc.getAuthService().authenticate("admin@something.com", "admin");
        waitForStoreToStart(RIO_GPKG_ID);
        rio = new SCSqliteHelper(testContext, "Rio").db();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        rio.close();
        sc.getNetworkService().cancelAllRequests();
        deleteDatabases();
    }

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(5, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

    @Test
    public void testGeoPackageStoreCanBeAccessedBySCSqliteHelper() {
        Cursor cursor = rio.query("SELECT COUNT(*) FROM police_stations;");
        cursor.moveToFirst();
        assertEquals("The police_stations table should only have 150 rows.", 150, cursor.getInt(0));
    }

    // test table creation functions
    @Test
    public void test_CreateSpatialIndex_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT CreateSpatialIndex(?, ?, ?);", new String[]{"police_stations", "geom", "fid"});
        cursor.moveToFirst();
        Cursor cursor2 = rio.query("SELECT COUNT(*) FROM rtree_police_stations_geom;");
        cursor2.moveToFirst();
        assertEquals("The spatial index tables should exist and be populated.", 150, cursor2.getInt(0));
    }

    // test geometry i/o functions
    @Test
    public void test_ST_AsText_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_AsText(geom) FROM police_stations LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("The geom should start with Point if ST_AsText worked.", cursor.getString(0).startsWith("Point"));
    }

    @Test
    public void test_ST_GeomFromText_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_GeomFromText('Point (-72.981321 18.42740796)');");
        cursor.moveToFirst();
        assertTrue("ST_GeomFromText should return a blob if it loaded correctly.", cursor.getColumnCount() == 1);
        assertTrue("ST_GeomFromText should return a blob if it loaded correctly.", cursor.getBlob(0).length > 0);

    }

    @Test
    public void test_ST_WKTToSQL_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_WKTToSQL('Point (0 0)');");
        cursor.moveToFirst();
        assertTrue("ST_WKTToSQL should return a blob if it loaded correctly.", cursor.getColumnCount() == 1);
        assertTrue("ST_WKTToSQL should return a blob if it loaded correctly.", cursor.getBlob(0).length > 0);
    }


    // test geometry inspection functions
    @Test
    public void test_ST_MinX_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_MinX(geom) FROM police_stations LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MinX should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_MaxX_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_MaxX(geom) FROM police_stations LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MaxX should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_MinY_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_MinY(geom) FROM police_stations LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MinY should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_MaxY_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_MaxY(geom) FROM police_stations LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MaxY should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_SRID_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_SRID(geom) FROM police_stations LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("ST_SRID should return 4326 geom column in police_stations.", 4326, cursor.getInt(0));
    }

    @Test
    public void test_ST_IsMeasured_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_IsMeasured(geom) FROM police_stations LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("ST_IsMeasured should return 0 for point features.", 0, cursor.getInt(0));
    }

    @Test
    public void test_ST_Is3d_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_Is3d(geom) FROM police_stations LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("ST_Is3d should return 0 for point features.", 0, cursor.getInt(0));
    }

    @Test
    public void test_ST_CoordDim_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_CoordDim(geom) FROM police_stations LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("The dimensions should be 2.", 2, cursor.getInt(0));
    }

    @Test
    public void test_ST_GeometryType_FunctionLoaded() {
        Cursor cursor = rio.query("SELECT ST_GeometryType(geom) FROM police_stations LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("The geometry type should be Point.", "Point", cursor.getString(0));
    }


}
