/**
 * Copyright 2015-2017 Boundless, http://boundlessgeo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.scutilities.Json;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * SCObjectMapper holds the single instance of Jackson's {@link com.fasterxml.jackson.databind.ObjectMapper} used by
 * this library.  It is configured once during the static initialization and is safe for use by multiple threads.
 */
public class SCObjectMapper {

    private ObjectMapper mapper;

    private SCObjectMapper() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JtsModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        mapper.registerModule(new JtsModule());
    }

    private static class SingletonHelper {
        private static final SCObjectMapper INSTANCE = new SCObjectMapper();
    }

    public static ObjectMapper getMapper() {
        return SCObjectMapper.SingletonHelper.INSTANCE.mapper;
    }
}
