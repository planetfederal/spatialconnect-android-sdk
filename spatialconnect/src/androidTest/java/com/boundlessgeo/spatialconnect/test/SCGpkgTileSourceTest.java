package com.boundlessgeo.spatialconnect.test;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;
import com.boundlessgeo.spatialconnect.tiles.SCGpkgTileSource;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;


public class SCGpkgTileSourceTest extends BaseTestCase {

    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        deleteDatabases();
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(remoteConfigFile);
        sc.startAllServices();
        sc.getAuthService().authenticate("admin@something.com", "admin");
        waitForStoreToStart(WHITEHORSE_GPKG_ID);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        deleteDatabases();
    }

    @Test
    public void testTileSourcesAreCreatedCorrectly() {
        SCDataStore store = sc.getDataService().getStoreById(WHITEHORSE_GPKG_ID);
        Map<String, SCGpkgTileSource> tileSources = ((GeoPackageAdapter) store.getAdapter()).getTileSources();
        assertEquals("The Whitehorse gpkg should have 1 tile source.", 1, tileSources.size());
        SCGpkgTileSource whiteHorseTileSource = tileSources.get("WhiteHorse");
        assertEquals("The Whitehorse gpkg should have 1 tile source named WhiteHorse.",
                "WhiteHorse",
                whiteHorseTileSource.getTableName()
        );
        assertEquals("The min zoom level of the WhiteHorse tile source should be 11.",
                11,
                (int) whiteHorseTileSource.getMinZoom()
        );
        assertEquals("The max zoom level of the WhiteHorse tile source should be 18.",
                18,
                (int) whiteHorseTileSource.getMaxZoom()
        );
        assertEquals("The srs id of the WhiteHorse tile source should be 3857.",
                3857,
                (int) whiteHorseTileSource.getSrsId()
        );
        assertEquals("The matrix of the WhiteHorse tile source should have 8 rows.",
                8,
                whiteHorseTileSource.getMatrix().size()
        );
    }

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(3, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

}
