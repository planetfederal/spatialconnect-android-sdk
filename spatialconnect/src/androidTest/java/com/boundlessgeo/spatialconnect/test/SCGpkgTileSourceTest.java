package com.boundlessgeo.spatialconnect.test;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.stores.GeoPackageStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCStoreStatusEvent;
import com.boundlessgeo.spatialconnect.tiles.SCGpkgTileSource;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;

public class SCGpkgTileSourceTest extends BaseTestCase {

    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.getConfigService().addConfigFilePath(localConfigFile.getAbsolutePath());
        sc.startAllServices();
        waitForStoreToStart(WHITEHORSE_GPKG_ID);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        deleteDatabases();
    }

    @Test
    public void testTileSourcesAreCreatedCorrectly() {
        GeoPackageStore store = (GeoPackageStore)sc.getDataService().getStoreByIdentifier(WHITEHORSE_GPKG_ID);
        Map<String, SCGpkgTileSource> tileSources = store.getTileSources();
        assertEquals("The Whitehorse gpkg should have 1 tile source.", 1, tileSources.size());
        SCGpkgTileSource whiteHorseTileSource = tileSources.get("WhiteHorse");
        assertEquals("The Whitehorse gpkg should have 1 tile source named WhiteHorse.",
                "WhiteHorse",
                whiteHorseTileSource.getTableName()
        );
        assertEquals("The min zoom level of the WhiteHorse tile source should be 12.",
                12,
                (int) whiteHorseTileSource.getMinZoom()
        );
        assertEquals("The max zoom level of the WhiteHorse tile source should be 12.",
                12,
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
        System.out.println("Waiting for store " + storeId + " to start");
        TestSubscriber testSubscriber = new TestSubscriber();
        Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                SCDataService dataService = sc.getDataService();
                SCDataStore store = dataService.getStoreByIdentifier(storeId);
                if (store != null) {
                    if (dataService.getStoreByIdentifier(storeId).getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)) {
                        subscriber.onCompleted();
                    } else {
                        dataService.storeEvents.autoConnect()
                                .timeout(2, TimeUnit.MINUTES)
                                .subscribe(new Action1<SCStoreStatusEvent>() {
                                    @Override
                                    public void call(SCStoreStatusEvent event) {
                                        if (event.getStoreId().equals(storeId) &&
                                                event.getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)) {
                                            subscriber.onCompleted();
                                        }
                                    }
                                });
                    }
                }
            }
        }).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
    }

}
