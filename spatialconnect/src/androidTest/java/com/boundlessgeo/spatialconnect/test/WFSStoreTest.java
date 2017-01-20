package com.boundlessgeo.spatialconnect.test;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.geometries.SCBoundingBox;
import com.boundlessgeo.spatialconnect.query.SCGeometryPredicateComparison;
import com.boundlessgeo.spatialconnect.query.SCPredicate;
import com.boundlessgeo.spatialconnect.query.SCQueryFilter;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.stores.SCDataStore;
import com.boundlessgeo.spatialconnect.stores.SCDataStoreStatus;
import com.boundlessgeo.spatialconnect.stores.WFSStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@Ignore //need to move to integration tests
public class WFSStoreTest extends BaseTestCase {

    private static SpatialConnect sc;
    private static final String WFS_STORE_ID = "71522e9b-3ec6-48c3-8d5c-57c8d14baf6a";

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.getConfigService().addConfig(localConfigFile);
        sc.startAllServices();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        deleteDatabases();
    }

    @Test
    public void testDataServiceStartsWFSStoreCorrectly() {
        boolean containsWFSStore = false;
        for (SCDataStore store : sc.getDataService().getActiveStores()) {
            if (store.getType().equals(WFSStore.TYPE)) containsWFSStore = true;
        }
        assertTrue("A wfs store should be running.", containsWFSStore);
        SCDataStore wfsStore = sc.getDataService().getStoreById(WFS_STORE_ID);
        assertEquals("The wfs store should be running", SCDataStoreStatus.SC_DATA_STORE_RUNNING, wfsStore.getStatus());
    }

    @Test
    public void testGetCapabilitesUrlIsBuiltCorrectly() {
        WFSStore store = (WFSStore) sc.getDataService().getStoreById(WFS_STORE_ID);
        assertEquals("The WFS store url was not built correctly.",
                "http://efc-dev.boundlessgeo.com:8080/geoserver/spatialconnect/ows?service=WFS&version=1.1.0&request=GetCapabilities",
                store.getGetCapabilitiesUrl()
        );
    }

    @Test
    public void testLayerNamesAreParsedFromCapabilities() {
        WFSStore store = (WFSStore) sc.getDataService().getStoreById(WFS_STORE_ID);
        assertTrue("There should be multiple layers.",
                store.layers().size() > 0
        );
    }

    @Test
    public void testQueryWithBbox() {
        SCBoundingBox bbox = new SCBoundingBox(-180, -90, 180, 90);
        SCQueryFilter filter = new SCQueryFilter(
                new SCPredicate(bbox, SCGeometryPredicateComparison.SCPREDICATE_OPERATOR_WITHIN)
        );
        WFSStore store = (WFSStore) sc.getDataService().getStoreById(WFS_STORE_ID);
        filter.addLayerId("baseball_team");
        TestSubscriber testSubscriber = new TestSubscriber();
        store.query(filter).timeout(5, TimeUnit.SECONDS).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent(); // it never completes, only times out
        assertTrue("The query observable should have emitted features", testSubscriber.getOnNextEvents().size() > 0);
        // TODO: test that the onNext events are valid scSpatialFeatures
    }


}

