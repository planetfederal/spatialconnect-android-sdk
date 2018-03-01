/**
 * Copyright 2018 Boundless, http://boundlessgeo.com
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

import com.boundlessgeo.spatialconnect.scutilities.WFSParser;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WFSParserTest extends BaseTestCase {
    private String wfstInsertResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wfs:WFS_TransactionResponse version=\"1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wfs https://exchange-farpoint.boundlessgeo.io/geoserver/schemas/wfs/1.0.0/WFS-transaction.xsd\">\n" +
            "    <wfs:InsertResult>\n" +
            "        <ogc:FeatureId fid=\"bb54c2ac1\"/>\n" +
            "    </wfs:InsertResult>\n" +
            "    <wfs:TransactionResult>\n" +
            "        <wfs:Status>\n" +
            "            <wfs:SUCCESS/>\n" +
            "        </wfs:Status>\n" +
            "    </wfs:TransactionResult>\n" +
            "</wfs:WFS_TransactionResponse>";

    @Test
    public void parseFeatureId() throws IOException, XmlPullParserException {
        InputStream is = new ByteArrayInputStream(wfstInsertResponse.getBytes(StandardCharsets.UTF_8.name()));
        WFSParser wfsParser = new WFSParser(is);
        assertEquals("Checking for proper featureId was parsed correctly",
                "bb54c2ac1",
                wfsParser.getFeatureId());
    }

    @Test
    public void parseTransactionResult() throws IOException, XmlPullParserException {
        InputStream is = new ByteArrayInputStream(wfstInsertResponse.getBytes(StandardCharsets.UTF_8.name()));
        WFSParser wfsParser = new WFSParser(is);
        assertTrue("WFST result should be successfully", wfsParser.isSuccess());
    }
}
