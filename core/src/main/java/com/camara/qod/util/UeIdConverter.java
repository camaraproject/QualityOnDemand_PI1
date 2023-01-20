/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 - 2023 Contributors | Deutsche Telekom AG to CAMARA a Series of LF
 *             Projects, LLC
 * The contributor of this file confirms his sign-off for the
 * Developer
 *             Certificate of Origin (http://developercertificate.org).
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

import com.camara.qod.api.model.UeId;
import com.camara.qod.exception.SessionApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Converter(autoApply = true)
@Slf4j
public class UeIdConverter implements AttributeConverter<UeId, String> {

  private static final String ERROR_MESSAGE = "Error during conversion of entity.";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(UeId attribute) {
    if (null == attribute) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
      throw new SessionApiException(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_MESSAGE);
    }
  }

  @Override
  public UeId convertToEntityAttribute(String dbData) {
    try {
      return objectMapper.readValue(dbData, UeId.class);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
      throw new SessionApiException(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_MESSAGE);
    }
  }
}
