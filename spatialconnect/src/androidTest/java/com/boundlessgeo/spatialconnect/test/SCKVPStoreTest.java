package com.boundlessgeo.spatialconnect.test;

import com.boundlessgeo.spatialconnect.db.SCKVPStore;

import org.junit.Before;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SCKVPStoreTest extends BaseTestCase {

    @Before
    public void resetDb() throws Exception {
        testContext.deleteDatabase(SCKVPStore.DATABASE_NAME);
    }

    @Test
    public void testPutIntoKeyValueDB() {
        SCKVPStore SCKVPStore = new SCKVPStore(testContext);
        SCKVPStore.put("stores.1234.type", "gpkg");
        Map<String, Object> value = SCKVPStore.getValuesForKeyPrefix("stores");
        assertEquals("There should be 1 value returned.", 1, value.size());
    }

    @Test
    public void testGetValueForKey() {
        SCKVPStore SCKVPStore = new SCKVPStore(testContext);
        SCKVPStore.put("stores.1234.type", "gpkg");
        Map<String, Object> value = SCKVPStore.getValueForKey("stores.1234.type");
        assertEquals("There should be 1 value returned for the key.", "gpkg", value.get("stores.1234.type"));
    }

    @Test
    public void testGetValuesForKeyPrefix() {
        SCKVPStore SCKVPStore = new SCKVPStore(testContext);
        SCKVPStore.put("stores.1234.type", "gpkg");
        SCKVPStore.put("stores.1234.isMainBundle", false);
        Map<String, Object> values = SCKVPStore.getValuesForKeyPrefix("stores");
        assertEquals("There should be 2 values returned.", 2, values.size());
    }

    @Test
    public void testBooleanDeserialize() {
        SCKVPStore SCKVPStore = new SCKVPStore(testContext);
        SCKVPStore.put("stores.1234.isMainBundle", Boolean.FALSE);
        Map<String, Object> stores = SCKVPStore.getValuesForKeyPrefix("stores.1234.isMainBundle");
        assertEquals("The value should be deserialized to a Boolean.",
                Boolean.FALSE, stores.get("stores.1234.isMainBundle")
        );
    }

    @Test
         public void testStringDeserialize() {
        SCKVPStore SCKVPStore = new SCKVPStore(testContext);
        SCKVPStore.put("stores.1234.type", "geojson");
        Map<String, Object> stores = SCKVPStore.getValuesForKeyPrefix("stores.1234.type");
        assertEquals("The value should be deserialized to a String.",
                "geojson", stores.get("stores.1234.type")
        );
    }

    @Test
    public void testLongDeserialize() {
        SCKVPStore SCKVPStore = new SCKVPStore(testContext);
        SCKVPStore.put("stores.1234.longNumber", Long.valueOf("100"));
        Map<String, Object> stores = SCKVPStore.getValuesForKeyPrefix("stores.1234.longNumber");
        assertEquals("The value should be deserialized to a Integer.",
                Integer.valueOf("100").getClass(), stores.get("stores.1234.longNumber").getClass()
        );
    }

    @Test
    public void testIntegerDeserialize() {
        SCKVPStore SCKVPStore = new SCKVPStore(testContext);
        SCKVPStore.put("stores.1234.integer", Integer.valueOf("100"));
        Map<String, Object> stores = SCKVPStore.getValuesForKeyPrefix("stores.1234.integer");
        assertEquals("The value should be deserialized to a Long.",
                Integer.valueOf("100"), stores.get("stores.1234.integer")
        );
    }

    @Test
    public void testDoubleDeserialize() {
        SCKVPStore SCKVPStore = new SCKVPStore(testContext);
        SCKVPStore.put("stores.1234.double", Double.valueOf("100.001"));
        Map<String, Object> stores = SCKVPStore.getValuesForKeyPrefix("stores.1234.double");
        assertEquals("The value should be deserialized to a Double.",
                Double.valueOf("100.001"), stores.get("stores.1234.double")
        );
    }

    @Test
    public void testFloatDeserialize() {
        SCKVPStore SCKVPStore = new SCKVPStore(testContext);
        SCKVPStore.put("stores.1234.float", Float.valueOf("100.001"));
        Map<String, Object> stores = SCKVPStore.getValuesForKeyPrefix("stores.1234.float");
        assertEquals("The value should be deserialized to a Double.",
                Double.valueOf("100.001"), stores.get("stores.1234.float")
        );
    }

    @Test
    public void testByteArrayDeserialize() throws IOException {
        SCKVPStore SCKVPStore = new SCKVPStore(testContext);
        InputStream is = testContext.getResources().openRawResource(R.raw.boundless_logo);
        FileOutputStream fos = new FileOutputStream(testConfigFile);
        byte[] data = new byte[is.available()];
        is.read(data);
        fos.write(data);
        is.close();
        fos.close();
        SCKVPStore.put("stores.1234.someImg", data);
        Map<String, Object> stores = SCKVPStore.getValuesForKeyPrefix("stores.1234.someImg");
        assertTrue("The value should be deserialized to the same byte array.",
                Arrays.equals(data, (byte[]) stores.get("stores.1234.someImg"))
        );
    }

}
