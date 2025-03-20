/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 - 2025 Contributors | Deutsche Telekom AG to CAMARA a Series of LF Projects, LLC
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

package com.camara.util;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.camara.network.api.model.ProblemDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * test data class.
 */
public abstract class TestData {

  public static final ObjectMapper objectMapper = new ObjectMapper().registerModule(
      new JavaTimeModule());

  public static String getAsJsonFormat(Object o) throws JsonProcessingException {
    return objectMapper.writeValueAsString(o);
  }

  /**
   * Mocks the request to add an Authorization - header.
   *
   * @param token the provided bearer token
   */
  public static void mockRequestWithToken(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(AUTHORIZATION, token);
    RequestAttributes requestAttributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(requestAttributes);
  }

  /**
   * Creates a {@link HttpClientErrorException}.
   *
   * @param status       the status
   * @param errorMessage the error message
   * @return the exception
   */
  public static HttpClientErrorException createHttpClientErrorException(int status, String errorMessage) {
    ProblemDetails problemDetails = new ProblemDetails();
    problemDetails.setDetail(errorMessage);
    problemDetails.setStatus(status);
    String json;
    try {
      json = objectMapper.writeValueAsString(problemDetails);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Invalid problem details");
    }
    byte[] jsonAsBytes = json.getBytes(StandardCharsets.UTF_8);

    return HttpClientErrorException.create(HttpStatusCode.valueOf(status), errorMessage, new HttpHeaders(), jsonAsBytes,
        StandardCharsets.UTF_8);
  }
}
