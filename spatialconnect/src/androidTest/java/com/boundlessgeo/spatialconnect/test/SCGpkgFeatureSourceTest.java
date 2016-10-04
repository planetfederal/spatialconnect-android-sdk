package com.boundlessgeo.spatialconnect.test;

import android.database.Cursor;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;
import com.boundlessgeo.spatialconnect.db.SCGpkgFeatureSource;
import com.boundlessgeo.spatialconnect.db.SCSqliteHelper;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.SCStoreStatusEvent;
import com.squareup.sqlbrite.BriteDatabase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;

@Ignore
public class SCGpkgFeatureSourceTest extends BaseTestCase {

    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(remoteConfigFile);
        sc.startAllServices();
        sc.getAuthService().authenticate("admin@something.com", "admin");
        waitForStoreToStart(RIO_GPKG_ID);
        waitForStoreToStart(WHITEHORSE_GPKG_ID);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        deleteDatabases();
    }

    @Test
    public void testGetGeoPackageContents() {
        SCDataStore rio = sc.getDataService().getStoreById(RIO_GPKG_ID);
        int contentsSize = ((GeoPackageAdapter) rio.getAdapter()).getGeoPackageContents().size();
        assertEquals("The Rio gpkg should have 27 rows in the gpkg_contents table.", 27, contentsSize);
        SCDataStore whitehorse = sc.getDataService().getStoreById(WHITEHORSE_GPKG_ID);
        contentsSize = ((GeoPackageAdapter) whitehorse.getAdapter()).getGeoPackageContents().size();
        assertEquals("The Whitehorse gpkg should only have 1 row in the gpkg_contents table.", 1, contentsSize);
    }

    @Test
    public void testGetFeatureSources() {
        SCDataStore store = sc.getDataService().getStoreById(RIO_GPKG_ID);
        int featureSourcesSize = ((GeoPackageAdapter) store.getAdapter()).getFeatureSources().size();
        assertEquals("The Haiti gpkg should have 27 feature tables.", 27, featureSourcesSize);
    }

    @Test
    public void testRtreeIndexIsCreated() {
        BriteDatabase db = new SCSqliteHelper(testContext, "Rio").db();
        SCDataStore store = sc.getDataService().getStoreById(RIO_GPKG_ID);
        Cursor cursor = null;
        for (SCGpkgFeatureSource source : ((GeoPackageAdapter) store.getAdapter()).getFeatureSources().values()) {
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
        System.out.println("Waiting for store " + storeId + " to start");
        TestSubscriber testSubscriber = new TestSubscriber();
        Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                SCDataService dataService = sc.getDataService();
                if (dataService.getStoreById(storeId).getStatus().equals(SCDataStoreStatus.SC_DATA_STORE_RUNNING)) {
                    subscriber.onCompleted();
                }
                else {
                    dataService.storeEvents.autoConnect()
                            .timeout(5, TimeUnit.MINUTES)
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
        }).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
    }


}
