package com.boundlessgeo.spatialconnect.test;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;
import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.db.SCKVPStore;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.stores.DefaultStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class DefaultStoreTest extends BaseTestCase {

    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(testConfigFile);
        sc.startAllServices();
        waitForStoreToStart("DEFAULT_STORE");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testContext.deleteDatabase("Haiti");
        testContext.deleteDatabase("Whitehorse");
        testContext.deleteDatabase(SCKVPStore.DATABASE_NAME);
        testContext.deleteDatabase("DEFAULT_STORE");
    }

    @Test
    public void testDefaultStoreGeoPackageIsInitialized() {
        DefaultStore defaultStore = (DefaultStore) sc.getDataService().getDefaultStore();
        assertEquals("The default store should have the adapter connected if it initialized properly.",
                defaultStore.getAdapter().getStatus(),
                SCDataAdapterStatus.DATA_ADAPTER_CONNECTED
        );
        assertEquals("The default store should have 2 form tables.",
                2, ((GeoPackageAdapter)defaultStore.getAdapter()).getGeoPackageContents().size());
    }

    @Test @Ignore
    public void testFormLayerCanBeDeleted() {
        DefaultStore defaultStore = (DefaultStore) sc.getDataService().getDefaultStore();
        defaultStore.deleteFormLayer("a_form_name");
        assertTrue("The default store should have 0 form tables.",
                0 == ((GeoPackageAdapter) defaultStore.getAdapter()).getGeoPackageContents().size());
    }

    @Test
    public void testFormSubmission() {
        DefaultStore defaultStore = (DefaultStore) sc.getDataService().getDefaultStore();
        SCSpatialFeature formSubmissionFeature = new SCSpatialFeature();
        // create a feature representing a form submission
        formSubmissionFeature.setLayerId("baseball_team");
        formSubmissionFeature.setStoreId("DEFAULT_STORE");
        HashMap properties = new HashMap<String, Object>();
        properties.put("team", "St. Louis Cardinals");
        properties.put("why", "Because they are awesome!");
        formSubmissionFeature.setProperties(properties);
        TestSubscriber testSubscriber = new TestSubscriber();
        defaultStore.create(formSubmissionFeature).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        assertTrue("The feature should have an id after it's created",
                !((SCSpatialFeature)testSubscriber.getOnNextEvents().get(0)).getId().equals("123"));
    }

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(1, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }

}
