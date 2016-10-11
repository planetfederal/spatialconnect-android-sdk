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
        waitForStoreToStart(HAITI_GPKG_ID);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        deleteDatabases();
    }

    @Test
    public void testGetGeoPackageContents() {
        SCDataStore whitehorse = sc.getDataService().getStoreById(HAITI_GPKG_ID);
        int contentsSize = ((GeoPackageAdapter) whitehorse.getAdapter()).getGeoPackageContents().size();
        assertEquals("The Haiti gpkg should only have 3 row in the gpkg_contents table.", 3, contentsSize);
    }

    @Test
    public void testGetFeatureSources() {
        SCDataStore store = sc.getDataService().getStoreById(HAITI_GPKG_ID);
        int featureSourcesSize = ((GeoPackageAdapter) store.getAdapter()).getFeatureSources().size();
        assertEquals("The Haiti gpkg should have 3 feature tables.", 3, featureSourcesSize);
    }

    @Ignore
    public void testRtreeIndexIsCreated() {
        BriteDatabase db = new SCSqliteHelper(testContext, HAITI_GPKG_ID).db();
        SCDataStore store = sc.getDataService().getStoreById(HAITI_GPKG_ID);
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
