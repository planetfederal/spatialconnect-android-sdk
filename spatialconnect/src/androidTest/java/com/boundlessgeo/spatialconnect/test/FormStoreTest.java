package com.boundlessgeo.spatialconnect.test;


import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.geometries.SCGeometry;
import com.boundlessgeo.spatialconnect.geometries.SCPoint;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.stores.FormStore;
import com.boundlessgeo.spatialconnect.stores.SCKeyTuple;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class FormStoreTest extends BaseTestCase {

    final String TEST_LAYER_NAME = "baseball_team";
    final String TEST_KEY_1 = "team";
    final String TEST_VALUE_1 = "Atlanta Braves";
    final String TEST_UPDATED_VALUE = "Cardinals";

    final String TEST_KEY_2 = "why";
    final String TEST_VALUE_2 = "Because they are awesome";

    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.getConfigService().addConfigFilePath(localConfigFile.getAbsolutePath());
        sc.startAllServices();
        waitForStoreToStart(FormStore.NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        testContext.deleteDatabase(FormStore.NAME);
    }

    @Test
    public void testFormStoreIsInitialized() {
        FormStore formStore =  sc.getDataService().getFormStore();

        assertEquals("The form store should have 1 form tables.",
                1, formStore.getGeoPackageContents().size());

        assertEquals("The from store table should be baseball_team.",
                "baseball_team", formStore.getGeoPackageContents().iterator().next().getTableName());
    }

    @Test
    public void testFormSubmissionAndUpdate() {

        FormStore formStore =  sc.getDataService().getFormStore();
        SCSpatialFeature formSubmissionFeature = new SCSpatialFeature();

        // create a feature representing a form submission
        formSubmissionFeature.setLayerId("baseball_team");
        formSubmissionFeature.setStoreId(FormStore.NAME);

        HashMap properties = new HashMap<String, Object>();
        properties.put(TEST_KEY_1, TEST_VALUE_1);
        properties.put(TEST_KEY_2, TEST_VALUE_2);
        formSubmissionFeature.setProperties(properties);

        TestSubscriber testSubscriber = new TestSubscriber();
        formStore.create(formSubmissionFeature).subscribe(testSubscriber);


        SCSpatialFeature submittedFeature = ((SCSpatialFeature)testSubscriber.getOnNextEvents().get(0));

        assertTrue("The feature should have an id after it's created",
                !((SCSpatialFeature)testSubscriber.getOnNextEvents().get(0)).getId().equals("123"));

        Map<String, Object> enteredProps =  ((SCSpatialFeature)testSubscriber.getOnNextEvents().get(0)).getProperties();
        assertEquals("Feature should have correct value submitted for key",
                TEST_VALUE_1, enteredProps.get(TEST_KEY_1));

        //Update newly created feature
        SCKeyTuple keys = new SCKeyTuple(FormStore.NAME, TEST_LAYER_NAME, submittedFeature.getId());

        formStore.queryById(keys).subscribe(testSubscriber);
        SCSpatialFeature feature = ((SCSpatialFeature)testSubscriber.getOnNextEvents().get(0));
        Map<String, Object> existingProps = feature.getProperties();
        existingProps.put(TEST_KEY_1, TEST_UPDATED_VALUE);

        formStore.update(feature).subscribe(testSubscriber);

        Map<String, Object> updatedProps =  ((SCSpatialFeature)testSubscriber.getOnNextEvents().get(0)).getProperties();
        assertTrue("Initial value should not equal updated value",
                !updatedProps.get(TEST_KEY_1).equals(TEST_VALUE_1));
    }

    @Test
    public void testFormLayerCanBeDeleted() {
        FormStore formStore =  sc.getDataService().getFormStore();
        formStore.deleteFormLayer(TEST_LAYER_NAME);
        assertTrue("The Form store should have 0 form tables.",
                0 == formStore.getGeoPackageContents().size());
    }

    @Test
    public void testGeometryFormSubmission() {

        FormStore formStore =  sc.getDataService().getFormStore();
        SCSpatialFeature formSubmissionFeature = new SCSpatialFeature();

        // create a feature representing a form submission
        formSubmissionFeature.setLayerId("fire_hydrant");
        formSubmissionFeature.setStoreId(FormStore.NAME);

        HashMap properties = new HashMap<>();
        Coordinate coordinate = new Coordinate(32,-32);
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 0);
        Geometry geometry = geometryFactory.createPoint(coordinate);
        SCPoint point = new SCPoint(geometry);

        properties.put("address","123 main street");
        properties.put("need_sevice", point);
        properties.put("service_range", point);
        formSubmissionFeature.setProperties(properties);

        TestSubscriber testSubscriber = new TestSubscriber();
        formStore.create(formSubmissionFeature).subscribe(testSubscriber);

        SCSpatialFeature submittedFeature = ((SCSpatialFeature)testSubscriber.getOnNextEvents().get(0));

        assertTrue("The feature should have an id after it's created",
                !((SCSpatialFeature)testSubscriber.getOnNextEvents().get(0)).getId().equals("123"));

        //Query newly created feature
        SCKeyTuple keys = new SCKeyTuple(FormStore.NAME, "fire_hydrant", submittedFeature.getId());
        formStore.queryById(keys).subscribe(testSubscriber);
        SCSpatialFeature feature = ((SCSpatialFeature)testSubscriber.getOnNextEvents().get(0));

        assertTrue("Submitted need_sevice geometry property should be SCGeometry object",
                feature.getProperties().get("need_sevice") instanceof SCGeometry);

        assertTrue("Submitted service_range geometry property should be SCGeometry object",
                feature.getProperties().get("service_range") instanceof SCGeometry);

        assertTrue("Submitted geometry property should be SCGeometry object",
                feature.getProperties().get("address").toString().equalsIgnoreCase("123 main street"));

    }

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(5, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }
}
