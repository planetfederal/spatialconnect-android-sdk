package com.boundlessgeo.spatialconnect.test;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.config.SCConfig;
import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.scutilities.Json.SCObjectMapper;
import com.boundlessgeo.spatialconnect.services.SCConfigService;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SCCacheTest extends BaseTestCase {

    private static SpatialConnect sc;
    private String jsonConfig = "{\n" +
            "\"stores\":[\n" +
            "{\n" +
            "\"store_type\": \"geojson\",\n" +
            "\"version\": \"1\",\n" +
            "\"uri\": \"all.geojson\",\n" +
            "\"id\":\"63602599-3ad3-439f-9c49-3c8a7579933b\",\n" +
            "\"name\":\"Simple\"\n" +
            "},\n" +
            "{\n" +
            "\"store_type\":\"geojson\",\n" +
            "\"version\":\"1\",\n" +
            "\"name\":\"20m Points\",\n" +
            "\"uri\":\"gz_2010_us_500_11_20m.json\",\n" +
            "\"id\":\"a5d93796-5026-46f7-a2ff-e5dec85d116b\"\n" +
            "},\n" +
            "{\n" +
            "\"store_type\":\"geojson\",\n" +
            "\"version\":\"1\",\n" +
            "\"name\":\"bars\",\n" +
            "\"uri\":\"https://s3.amazonaws.com/test.spacon/bars.geojson\",\n" +
            "\"id\":\"a5d93796-5026-46f7-a2ff-e5dec85d116c\"\n" +
            "},\n" +
            "{\n" +
            "\"store_type\":\"gpkg\",\n" +
            "\"version\":\"1\",\n" +
            "\"name\":\"Haiti\",\n" +
            "\"uri\":\"https://s3.amazonaws.com/test.spacon/haiti4mobile.gpkg\",\n" +
            "\"id\":\"f6dcc750-1349-46b9-a324-0223764d46d1\"\n" +
            "},\n" +
            "{\n" +
            "\"store_type\":\"gpkg\",\n" +
            "\"version\":\"1\",\n" +
            "\"name\":\"Whitehorse\",\n" +
            "\"uri\":\"https://portal.opengeospatial.org/files/63156\",\n" +
            "\"id\":\"ba293796-5026-46f7-a2ff-e5dec85heh6b\"\n" +
            "},\n" +
            "{\n" +
            "\"store_type\":\"wfs\",\n" +
            "\"version\":\"1.1.0\",\n" +
            "\"name\":\"WFS Demo\",\n" +
            "\"default_layers\":[\"osm:polygon_barrier\"],\n" +
            "\"uri\":\"http://demo.boundlessgeo.com/geoserver/wfs\",\n" +
            "\"id\":\"0f193979-b871-47cd-b60d-e271d6504359\"\n" +
            "}\n" +
            "]\n" +
            "}";

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.startAllServices();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        sc.stopAllServices();
    }

    @Test
    public void testCrud() {
        try {
            SCConfigService cs = sc.getConfigService();
            SCConfig c = SCObjectMapper.getMapper().readValue(jsonConfig, SCConfig.class);
            cs.setCachedConfig(c);
            SCConfig config2 = cs.getCachedConfig();

            //CREATE
            SCStoreConfig storeConfig = new SCStoreConfig();
            storeConfig.setType("geojson");
            storeConfig.setUniqueID("foo123");
            storeConfig.setVersion("1");
            storeConfig.setUri("https://foo.com");
            storeConfig.setName("fooname");

            config2.addStore(storeConfig);
            cs.setCachedConfig(config2);

            SCConfig config3 = cs.getCachedConfig();

            assertEquals("Cached config should have +1 of original cached config.",
                    config3.getStores().size(),
                    c.getStores().size() + 1);

            //UPDATE
            storeConfig.setName("fooNameChange");
            config3.updateStore(storeConfig);
            cs.setCachedConfig(config3);

            SCConfig config4 = cs.getCachedConfig();
            boolean updateFound = false;

            for (SCStoreConfig c4store : config4.getStores()) {
                if ( c4store.getName().equalsIgnoreCase("fooNameChange")) {
                    updateFound = true;
                    break;
                }
            }

            assertTrue("Store updated in remote config cache", updateFound);

            //DELETE
            config4.removeStore("foo123");
            cs.setCachedConfig(config4);
            SCConfig config5 = cs.getCachedConfig();
            boolean notFound = true;
            for (SCStoreConfig c5store : config5.getStores()) {
                if ( c5store.getUniqueID().equalsIgnoreCase("foo123")) {
                    notFound = true;
                    break;
                }
            }

            assertTrue("Deleted store not found", notFound);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
