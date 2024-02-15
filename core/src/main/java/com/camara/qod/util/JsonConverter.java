/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 - 2024 Contributors | Deutsche Telekom AG to CAMARA a Series of LF Projects, LLC
 *
 * The contributor of this file confirms his sign-off for the Developer Certificate of Origin
 *             (https://developercertificate.org).
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package com.camara.qod.util;

import com.camara.qod.exception.QodApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Converter
@Slf4j
@RequiredArgsConstructor
public class JsonConverter<T> implements AttributeConverter<T, String> {

  private static final String ERROR_MESSAGE = "Error during conversion of entity.";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final Class<T> typeClass;


  @Override
  public String convertToDatabaseColumn(T object) {
    if (null == object) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
      throw new QodApiException(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_MESSAGE);
    }
  }

  @Override
  public T convertToEntityAttribute(String json) {
    if (null == json) {
      return null;
    }
    try {
      return objectMapper.readValue(json, typeClass);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
      throw new QodApiException(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_MESSAGE);
    }
  }
}
