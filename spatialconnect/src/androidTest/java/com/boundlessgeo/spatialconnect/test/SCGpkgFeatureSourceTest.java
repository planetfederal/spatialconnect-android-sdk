package com.boundlessgeo.spatialconnect.test;

import android.database.Cursor;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;
import com.boundlessgeo.spatialconnect.db.SCGpkgFeatureSource;
import com.boundlessgeo.spatialconnect.db.SCSqliteHelper;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.squareup.sqlbrite.BriteDatabase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;


public class SCGpkgFeatureSourceTest extends BaseTestCase {

    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(testConfigFile);
        sc.startAllServices();
        waitForStoreToStart(WHITEHORSE_GPKG_ID);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        deleteDatabases();
    }

    @Test
    public void testGetGeoPackageContents() {
        SCDataStore haiti = sc.getDataService().getStoreById(HAITI_GPKG_ID);
        int contentsSize = ((GeoPackageAdapter) haiti.getAdapter()).getGeoPackageContents().size();
        assertEquals("The Haiti gpkg should only have 3 rows in the gpkg_contents table.", 3, contentsSize);
        SCDataStore whitehorse = sc.getDataService().getStoreById(WHITEHORSE_GPKG_ID);
        contentsSize = ((GeoPackageAdapter) whitehorse.getAdapter()).getGeoPackageContents().size();
        assertEquals("The Whitehorese gpkg should only have 1 row in the gpkg_contents table.", 1, contentsSize);
    }

    @Test
    public void testGetFeatureSources() {
        SCDataStore store = sc.getDataService().getStoreById(HAITI_GPKG_ID);
        int featureSourcesSize = ((GeoPackageAdapter) store.getAdapter()).getFeatureSources().size();
        assertEquals("The Haiti gpkg should only have 3 feature tables.", 3, featureSourcesSize);
    }

    @Test
    public void testRtreeIndexIsCreated() {
        BriteDatabase db = new SCSqliteHelper(testContext, "gpkg1").db();
        SCDataStore store = sc.getDataService().getStoreById(HAITI_GPKG_ID);
        Cursor cursor = null;
        for (SCGpkgFeatureSource source : ((GeoPackageAdapter) store.getAdapter()).getFeatureSources()) {
            try {
                 cursor = db.query(
                        "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?",
                        new String[]{"rtree_" + source.getTableName() + "_" + source.getGeomColumnName()}
                );
                cursor.moveToFirst();
                assertEquals("The rtree tables should exist.", 1, cursor.getInt(0));
            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(1, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

}
