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
    private static BriteDatabase haiti;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(testConfigFile);
        sc.startAllServices();
        waitForStoreToStart(HAITI_GPKG_ID);
        haiti = new SCSqliteHelper(testContext, "gpkg1").db();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        haiti.close();
        testContext.deleteDatabase("gpkg1");
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
        Cursor cursor = haiti.query("SELECT COUNT(*) FROM point_features;");
        cursor.moveToFirst();
        assertEquals("The point_features table should only have 1000 rows.", 1000, cursor.getInt(0));
    }

    // test table creation functions
    @Test
    public void test_CreateSpatialIndex_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT CreateSpatialIndex(?, ?, ?);", new String[]{"point_features", "the_geom", "fid"});
        cursor.moveToFirst();
        Cursor cursor2 = haiti.query("SELECT COUNT(*) FROM rtree_point_features_the_geom;");
        cursor2.moveToFirst();
        assertEquals("The spatial index tables should exist and be populated.", 1000, cursor2.getInt(0));
    }

    // test geometry i/o functions
    @Test
    public void test_ST_AsText_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_AsText(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("The geom should start with Point if ST_AsText worked.", cursor.getString(0).startsWith("Point"));
    }

    @Test
    public void test_ST_GeomFromText_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_GeomFromText('Point (-72.981321 18.42740796)');");
        cursor.moveToFirst();
        assertTrue("ST_GeomFromText should return a blob if it loaded correctly.", cursor.getColumnCount() == 1);
        assertTrue("ST_GeomFromText should return a blob if it loaded correctly.", cursor.getBlob(0).length > 0);

    }

    @Test
    public void test_ST_WKTToSQL_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_WKTToSQL('Point (0 0)');");
        cursor.moveToFirst();
        assertTrue("ST_WKTToSQL should return a blob if it loaded correctly.", cursor.getColumnCount() == 1);
        assertTrue("ST_WKTToSQL should return a blob if it loaded correctly.", cursor.getBlob(0).length > 0);
    }


    // test geometry inspection functions
    @Test
    public void test_ST_MinX_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_MinX(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MinX should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_MaxX_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_MaxX(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MaxX should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_MinY_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_MinY(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MinY should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_MaxY_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_MaxY(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertTrue("ST_MaxY should return a number if it loaded correctly.", cursor.getCount() == 1);
    }

    @Test
    public void test_ST_SRID_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_SRID(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("ST_SRID should return 4326 the_geom column in point_features.", 4326, cursor.getInt(0));
    }

    @Test
    public void test_ST_IsMeasured_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_IsMeasured(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("ST_IsMeasured should return 0 for point features.", 0, cursor.getInt(0));
    }

    @Test
    public void test_ST_Is3d_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_Is3d(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("ST_Is3d should return 0 for point features.", 0, cursor.getInt(0));
    }

    @Test
    public void test_ST_CoordDim_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_CoordDim(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("The dimensions should be 2.", 2, cursor.getInt(0));
    }

    @Test
    public void test_ST_GeometryType_FunctionLoaded() {
        Cursor cursor = haiti.query("SELECT ST_GeometryType(the_geom) FROM point_features LIMIT 1;");
        cursor.moveToFirst();
        assertEquals("The geometry type should be Point.", "Point", cursor.getString(0));
    }


}
