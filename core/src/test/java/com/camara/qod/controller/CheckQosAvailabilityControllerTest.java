/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 Contributors | Deutsche Telekom AG to CAMARA a Series of LF Projects, LLC
 * The contributor of this file confirms his sign-off for the
 * Developer Certificate of Origin (http://developercertificate.org).
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

package com.camara.qod.controller;

import static com.camara.qod.util.SessionsTestData.AVAILABILITY_URI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camara.qod.api.CheckQosAvailabilityApiController;
import com.camara.qod.api.model.CheckQosAvailabilityResponse;
import com.camara.qod.api.model.CheckQosAvailabilityResponseQosProfiles;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.service.QodService;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@WebMvcTest(CheckQosAvailabilityApiController.class)
@Import(value = {
    CheckQosAvailabilityController.class,
    ExceptionHandlerAdvice.class
})
@ContextConfiguration(classes = CheckQosAvailabilityApiController.class)
@AutoConfigureMockMvc
class CheckQosAvailabilityControllerTest {

  @MockBean
  QodService qodService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void notificationsPost_ok() throws Exception {
    when(qodService.checkQosAvailability(any())).thenReturn(getSampleResponse());

    mockMvc.perform(MockMvcRequestBuilders
            .post(AVAILABILITY_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getCheckQosAvailabilityRequest()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isCreated());
  }

  @Test
  void notificationsPost_NotFound() throws Exception {
    when(qodService.checkQosAvailability(any())).thenThrow(
        new SessionApiException(HttpStatus.NOT_FOUND,
            "Resource not found"));

    mockMvc.perform(MockMvcRequestBuilders
            .post(AVAILABILITY_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getCheckQosAvailabilityRequest()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.message")
            .value("Resource not found"));
  }

  private String getCheckQosAvailabilityRequest() {
    return "{\"ueId\": \"123456789@domain.com\"}";
  }

  private CheckQosAvailabilityResponse getSampleResponse() {
    CheckQosAvailabilityResponseQosProfiles[] profiles =
        new CheckQosAvailabilityResponseQosProfiles[]{
            new CheckQosAvailabilityResponseQosProfiles().qos(QosProfile.THROUGHPUT_S),
            new CheckQosAvailabilityResponseQosProfiles().qos(QosProfile.THROUGHPUT_M)};

    CheckQosAvailabilityResponse response = new CheckQosAvailabilityResponse();
    response.setQosProfiles(Arrays.asList(profiles));
    return response;
  }
}
