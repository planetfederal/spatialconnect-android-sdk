/**
 * Copyright 2015-2017 Boundless, http://boundlessgeo.com
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.test;

import com.boundlessgeo.spatialconnect.SpatialConnect;
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.boundlessgeo.spatialconnect.stores.GeoJsonStore;
import com.boundlessgeo.spatialconnect.style.SCStyle;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;

public class SCStyleTest extends BaseTestCase {

    private static SpatialConnect sc;

    @BeforeClass
    public static void setUp() throws Exception {
        sc = SpatialConnect.getInstance();
        sc.initialize(activity);
        sc.getConfigService().addConfigFilePath(localConfigFile.getAbsolutePath());
        sc.startAllServices();
        waitForStoreToStart(BARS_GEO_JSON_ID);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        HttpHandler.getInstance().cancelAllRequests();
        testContext.deleteFile(BARS_GEO_JSON_ID + ".json");
    }

    @Test
    public void testStyleConversion() {
        GeoJsonStore store = (GeoJsonStore) sc.getDataService().getStoreByIdentifier(BARS_GEO_JSON_ID);
        SCStyle style = store.getStyle();
        assertEquals("Fill color for store should be #34fb71",
                "#34fb71",
                style.getFillColor()
        );
        assertEquals("Fill opacity for store should be 0.3",
                0.3f,
                style.getFillOpacity(), 0.0f
        );
        assertEquals("Stroke color for store should be #4357fb",
                "#4357fb",
                style.getStrokeColor()
        );
        assertEquals("Stroke color for store should be 1",
                1f,
                style.getStrokeOpacity(),0.0f
        );
        assertEquals("Icon color for store should be #3371fb",
                "#3371fb",
                style.getIconColor()
        );

    }

    private static void waitForStoreToStart(final String storeId) {
        TestSubscriber testSubscriber = new TestSubscriber();
        sc.getDataService().storeStarted(storeId).timeout(5, TimeUnit.MINUTES).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }
}
