package com.boundlessgeo.spatialconnect.test;

import com.boundlessgeo.spatialconnect.services.SCDataService;
import com.boundlessgeo.spatialconnect.services.SCServiceManager;

public class ServicesTest extends BaseTestCase {

    public void testDataServiceInitialization() {
        SCDataService dataService = new SCDataService();
        assertTrue("The data service should have 2 supported stores.",
                dataService.getSupportedStoreKeys().size() == 2
        );
    }

    public void testServiceManagerSetup() {
        SCServiceManager serviceManager = new SCServiceManager(testContext);
        assertTrue("At least one of the default services should have been initialized.",
                !serviceManager.getServices().isEmpty()
        );
        assertEquals("3 default services should have been initialized (data, network, and sensor)",
                3, serviceManager.getServices().size()
        );
        assertEquals("There should be only 2 supported data stores: geojson.1 and geopackage.1",
                2,
                serviceManager.getDataService().getSupportedStoreKeys().size()
        );
        assertTrue("The geojson.1 store should be in the list of supported stores.",
                serviceManager.getDataService().getSupportedStoreKeys().contains("geojson.1")
        );
        assertTrue("The geopackage.1 store should be in the list of supported stores.",
                serviceManager.getDataService().getSupportedStoreKeys().contains("geopackage.1")
        );
    }
}
