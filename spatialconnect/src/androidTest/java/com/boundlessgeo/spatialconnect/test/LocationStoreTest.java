package com.boundlessgeo.spatialconnect.test;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;
import com.boundlessgeo.spatialconnect.dataAdapter.SCDataAdapterStatus;
import com.boundlessgeo.spatialconnect.geometries.SCPoint;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.stores.LocationStore;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;

@Ignore
public class LocationStoreTest extends BaseTestCase {

    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.addConfig(localConfigFile);
        sc.startAllServices();
        waitForStoreToStart(LocationStore.NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        testContext.deleteDatabase(LocationStore.NAME);
    }

    @Test
    public void testLocationStoreIsInitialized() {
        LocationStore locationStore =  sc.getDataService().getLocationStore();
        assertEquals("The location store should have the adapter connected if it initialized properly.",
                locationStore.getAdapter().getStatus(),
                SCDataAdapterStatus.DATA_ADAPTER_CONNECTED
        );

        assertEquals("The location store should have 1  table.",
                1, ((GeoPackageAdapter)locationStore.getAdapter()).getGeoPackageContents().size());
    }

    @Test
    public void testAddingLocation() {

        Coordinate coordinate = new Coordinate(32,-32);
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 0);
        Geometry geometry = geometryFactory.createPoint(coordinate);
        SCPoint point = new SCPoint(geometry);
        point.getProperties().put("timestamp", System.currentTimeMillis()); //add timestamp
        point.getProperties().put("accuracy", "GPS");

        TestSubscriber testSubscriber = new TestSubscriber();
        LocationStore locationStore =  sc.getDataService().getLocationStore();
        locationStore.create(point).subscribe(testSubscriber);


        Map<String, Object> row =  ((SCSpatialFeature)testSubscriber.getOnNextEvents().get(0)).getProperties();
        assertEquals("location store should have correct properties",
                "GPS", row.get("accuracy"));
    }

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(5, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }
}
