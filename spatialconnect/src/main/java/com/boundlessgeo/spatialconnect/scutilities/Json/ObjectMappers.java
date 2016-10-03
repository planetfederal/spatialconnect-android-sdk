/**
 * Copyright 2015-2016 Boundless, http://boundlessgeo.com
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
 * See the License for the specific language governing permissions and limitations under the
 * License
 */
package com.boundlessgeo.spatialconnect.scutilities.Json;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ObjectMappers {

  private static ObjectMapper jtsMapper;
  private static ObjectMapper outputMapper;
  private static ObjectMapper genericMapper;
  private static ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new JtsModule());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    jtsMapper = new ObjectMapper();
    jtsMapper.registerModule(new JtsModule());

    outputMapper = new ObjectMapper();
    outputMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    outputMapper.enable(SerializationFeature.INDENT_OUTPUT);

    genericMapper = new ObjectMapper();
  }

  public static ObjectMapper getJTSMapper() {
    return jtsMapper;
  }

  public static ObjectMapper getGenericMapper() {
    return genericMapper;
  }

  public static ObjectMapper getMapper() {
    return mapper;
  }
}
