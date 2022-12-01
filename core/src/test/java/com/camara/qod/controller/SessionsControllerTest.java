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

import static com.camara.qod.util.SessionsTestData.SESSION_URI;
import static com.camara.qod.util.SessionsTestData.SESSION_UUID;
import static com.camara.qod.util.SessionsTestData.createSessionInfoSample;
import static com.camara.qod.util.SessionsTestData.getTestSessionAddrInvalid;
import static com.camara.qod.util.SessionsTestData.getTestSessionNetworkInvalid;
import static com.camara.qod.util.SessionsTestData.getTestSessionRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camara.qod.api.SessionsApiController;
import com.camara.qod.exception.ExceptionHandlerAdvice;
import com.camara.qod.exception.SessionApiException;
import com.camara.qod.service.QodService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@WebMvcTest(SessionsApiController.class)
@Import(value = {
    SessionsController.class,
    ExceptionHandlerAdvice.class
})
@ContextConfiguration(classes = SessionsApiController.class)
class SessionsControllerTest {

  @MockBean
  QodService qodService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void createSession_ok() throws Exception {
    when(qodService.createSession(any())).thenReturn(createSessionInfoSample());
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getTestSessionRequest()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.notificationAuthToken")
            .value("c8974e592c2fa383d4a3960714"))
        .andExpect(jsonPath("$.id")
            .value(SESSION_UUID));
  }

  @Test
  void getSession_ok() throws Exception {
    when(qodService.getSession(any())).thenReturn(createSessionInfoSample());
    mockMvc.perform(MockMvcRequestBuilders
            .get(SESSION_URI + "/" + SESSION_UUID)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notificationAuthToken")
            .value("c8974e592c2fa383d4a3960714"))
        .andExpect(jsonPath("$.id")
            .value(SESSION_UUID));
  }

  @Test
  void deleteSession_ok() throws Exception {
    when(qodService.deleteSession(any())).thenReturn(createSessionInfoSample());
    mockMvc.perform(MockMvcRequestBuilders
            .delete(SESSION_URI + "/" + SESSION_UUID)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isNoContent());
  }

  @Test
  void createSession_ServiceUnavailable() throws Exception {
    when(qodService.createSession(any())).thenThrow(
        new SessionApiException(HttpStatus.SERVICE_UNAVAILABLE,
            "The service is currently not available"));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getTestSessionRequest()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message")
            .value("The service is currently not available"));
  }

  @Test
  void createSession_BadRequest_SegmentNotAllowed() throws Exception {
    when(qodService.createSession(any())).thenThrow(new SessionApiException(HttpStatus.BAD_REQUEST,
        "A network segment for ueAddr is not allowed in the current configuration: "
            + "198.51.100.1 is not allowed, but 123.45.678.9 is allowed."));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getTestSessionRequest()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("A network segment for ueAddr is not allowed in the current "
                + "configuration: 198.51.100.1 is not allowed, but 123.45.678.9 is allowed."));
  }

  @Test
  void createSession_BadRequest_NetworkValid() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getTestSessionNetworkInvalid()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Network specification not valid 198.51.100.1/18"));
  }

  @Test
  void createSession_BadRequest_ValidationAddressFailed() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getTestSessionAddrInvalid()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Validation failed for parameter 'ueId.ipv4addr'"));
  }

  @Test
  void createSession_BadRequest_PortsNotValid() throws Exception {
    when(qodService.createSession(any())).thenThrow(new SessionApiException(HttpStatus.BAD_REQUEST,
        "Ports specification not valid 5010-5020,5021,5022,AB"));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getTestSessionRequest()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Ports specification not valid 5010-5020,5021,5022,AB"));
  }

  @Test
  void createSession_Conflict_AlreadyActive() throws Exception {
    when(qodService.createSession(any())).thenThrow(new SessionApiException(HttpStatus.CONFLICT,
        "Found session XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXcfbb already active until "
            + "2022-10-14T11:12:43Z"));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getTestSessionRequest()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message")
            .value(
                "Found session XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXcfbb already active until "
                    + "2022-10-14T11:12:43Z"));
  }

  @Test
  void createSession_Conflict_SessionNotAvailable() throws Exception {
    when(qodService.createSession(any())).thenThrow(new SessionApiException(HttpStatus.CONFLICT,
        "Requested QoS session is currently not available"));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getTestSessionRequest()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message")
            .value("Requested QoS session is currently not available"));
  }

  @Test
  void createSession_InternalServerError() throws Exception {
    when(qodService.createSession(any())).thenThrow(
        new SessionApiException(HttpStatus.INTERNAL_SERVER_ERROR,
            "No valid subscription ID was provided in NEF/SCEF response"));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getTestSessionRequest()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message")
            .value(
                "No valid subscription ID was provided in NEF/SCEF response"));
  }

  @Test
  void getSession_NotFound() throws Exception {
    when(qodService.getSession(any())).thenThrow(
        new SessionApiException(HttpStatus.NOT_FOUND,
            "QoD session not found for session ID: 963ab9f5-26e8-48b9-a56e-52ecdeaa9172"));
    mockMvc.perform(MockMvcRequestBuilders
            .get(SESSION_URI + "/" + SESSION_UUID)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message")
            .value("QoD session not found for session ID: 963ab9f5-26e8-48b9-a56e-52ecdeaa9172"));
  }

  @Test
  void deleteSession_ServiceUnavailable() throws Exception {
    when(qodService.deleteSession(any())).thenThrow(
        new SessionApiException(HttpStatus.SERVICE_UNAVAILABLE,
            "The service is currently not available"));
    mockMvc.perform(MockMvcRequestBuilders
            .delete(SESSION_URI + "/" + SESSION_UUID)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message")
            .value("The service is currently not available"));
  }

  @Test
  void deleteSession_ServiceNotFound() throws Exception {
    when(qodService.deleteSession(any())).thenThrow(
        new SessionApiException(HttpStatus.NOT_FOUND,
            "QoD session not found for session ID: 963ab9f5-26e8-48b9-a56e-52ecdeaa9172"));
    mockMvc.perform(MockMvcRequestBuilders
            .delete(SESSION_URI + "/" + SESSION_UUID)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message")
            .value("QoD session not found for session ID: 963ab9f5-26e8-48b9-a56e-52ecdeaa9172"));
  }


}
