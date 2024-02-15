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

package com.camara.qod.controller;

import static com.camara.qod.exception.ErrorCode.INVALID_INPUT;
import static com.camara.qod.exception.ErrorCode.NOT_ALLOWED;
import static com.camara.qod.util.SessionsTestData.SESSION_URI;
import static com.camara.qod.util.SessionsTestData.SESSION_UUID;
import static com.camara.qod.util.SessionsTestData.SESSION_XML_REQUEST;
import static com.camara.qod.util.SessionsTestData.createSessionInfoSample;
import static com.camara.qod.util.SessionsTestData.createTestSession;
import static com.camara.qod.util.SessionsTestData.createTestSessionWithInvalidAppServerNetwork;
import static com.camara.qod.util.SessionsTestData.createValidTestSession;
import static com.camara.qod.util.TestData.getAsJsonFormat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camara.qod.annotation.UnsecuredWebMvcTest;
import com.camara.qod.api.SessionsApiController;
import com.camara.qod.exception.ExceptionHandlerAdvice;
import com.camara.qod.exception.QodApiException;
import com.camara.qod.security.SecurityConfig;
import com.camara.qod.service.SessionService;
import com.camara.qod.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@UnsecuredWebMvcTest(controllers = SessionsApiController.class)
@Import(value = {
    SessionsController.class,
    ExceptionHandlerAdvice.class,
    SecurityConfig.class

})
@ContextConfiguration(classes = SessionsApiController.class)
class SessionsControllerTest {

  @MockBean
  SessionService sessionService;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  ValidationService validationService;

  /* create session */

  @Test
  void testCreateSession_Created_201() throws Exception {
    when(sessionService.createSession(any())).thenReturn(createSessionInfoSample());
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createValidTestSession())))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.webhook.notificationAuthToken")
            .value("c8974e592c2fa383d4a3960714"))
        .andExpect(jsonPath("$.sessionId")
            .value(SESSION_UUID));
  }

  @Test
  void testCreateSession_BadRequest_SegmentNotAllowed_400() throws Exception {
    when(sessionService.createSession(any())).thenThrow(new QodApiException(HttpStatus.BAD_REQUEST,
        "A network segment for ueAddr is not allowed in the current configuration: "
            + "198.51.100.1 is not allowed, but 123.45.678.9 is allowed."));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createValidTestSession())))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("A network segment for ueAddr is not allowed in the current "
                + "configuration: 198.51.100.1 is not allowed, but 123.45.678.9 is allowed."));
  }

  @Test
  void testCreateSession_BadRequest_InvalidIpv4_400() throws Exception {
    doCallRealMethod().when(validationService).validate(any());
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createTestSessionWithInvalidAppServerNetwork())))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Network specification for device.ipv4Address.publicAddress not valid 172.24.11.4/18"));
  }

  @Test
  void testCreateSession_BadRequest_InvalidPortsRange_400() throws Exception {
    when(sessionService.createSession(any())).thenThrow(new QodApiException(HttpStatus.BAD_REQUEST,
        "Ports specification not valid 5010-5020,5021,5022,AB"));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createValidTestSession())))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Ports specification not valid 5010-5020,5021,5022,AB"));
  }

  @Test
  void testCreateSession_BadRequest_InvalidDuration_400() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createTestSession(0))))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Validation failed for parameter 'duration'"));

    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createTestSession(86401))))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Validation failed for parameter 'duration'"));
  }

  @Test
  void testCreateSession_MethodNotAllowed_405() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders
            .get(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createValidTestSession())))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code")
            .value(NOT_ALLOWED.name()))
        .andExpect(jsonPath("$.message")
            .value("Request method 'GET' is not supported"));
  }

  @Test
  void testCreateSession_NotAcceptable_406() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_XML)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createValidTestSession())))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isNotAcceptable())
        .andExpect(jsonPath("$.code")
            .value(INVALID_INPUT.name()))
        .andExpect(jsonPath("$.message")
            .value("No acceptable representation. Supported media types: [application/json]"));
  }

  @Test
  void testCreateSession_Conflict_SessionAlreadyActive_409() throws Exception {
    when(sessionService.createSession(any())).thenThrow(new QodApiException(HttpStatus.CONFLICT,
        "Found session XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXcfbb already active until "
            + "2022-10-14T11:12:43Z"));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createValidTestSession())))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message")
            .value(
                "Found session XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXcfbb already active until "
                    + "2022-10-14T11:12:43Z"));
  }

  @Test
  void testCreateSession_Conflict_SessionNotAvailable_409() throws Exception {
    when(sessionService.createSession(any())).thenThrow(new QodApiException(HttpStatus.CONFLICT,
        "Requested QoS session is currently not available"));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createValidTestSession())))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message")
            .value("Requested QoS session is currently not available"));
  }

  @Test
  void testCreateSession_UnsupportedMediaType_415() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .content(SESSION_XML_REQUEST))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code")
            .value(INVALID_INPUT.name()))
        .andExpect(jsonPath("$.message")
            .value("Content-Type 'application/xml' is not supported"));
  }

  @Test
  void testCreateSession_InternalServerError_500() throws Exception {
    when(sessionService.createSession(any())).thenThrow(
        new QodApiException(HttpStatus.INTERNAL_SERVER_ERROR,
            "No valid subscription ID was provided in NEF/SCEF response"));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createValidTestSession())))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message")
            .value(
                "No valid subscription ID was provided in NEF/SCEF response"));
  }

  @Test
  void testCreateSession_ServiceUnavailable_503() throws Exception {
    when(sessionService.createSession(any())).thenThrow(
        new QodApiException(HttpStatus.SERVICE_UNAVAILABLE,
            "The service is currently not available"));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createValidTestSession())))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message")
            .value("The service is currently not available"));
  }

  /* get session */

  @Test
  void testGetSession_Ok_200() throws Exception {
    when(sessionService.getSession(any())).thenReturn(createSessionInfoSample());
    mockMvc.perform(MockMvcRequestBuilders
            .get(SESSION_URI + "/" + SESSION_UUID)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.webhook.notificationAuthToken")
            .value("c8974e592c2fa383d4a3960714"))
        .andExpect(jsonPath("$.sessionId")
            .value(SESSION_UUID));
  }

  @Test
  void testGetSession_NotFound_404() throws Exception {
    when(sessionService.getSession(any())).thenThrow(
        new QodApiException(HttpStatus.NOT_FOUND,
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
  void testGetSession_MethodNotAllowed_405() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI + "/" + SESSION_UUID)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code")
            .value(NOT_ALLOWED.name()))
        .andExpect(jsonPath("$.message")
            .value("Request method 'POST' is not supported"));
  }

  @Test
  void testGetSession_NotAcceptable_406() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders
            .get(SESSION_URI + "/" + SESSION_UUID)
            .accept(MediaType.APPLICATION_XML)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isNotAcceptable())
        .andExpect(jsonPath("$.code")
            .value(INVALID_INPUT.name()))
        .andExpect(jsonPath("$.message")
            .value("No acceptable representation. Supported media types: [application/json]"));
  }

  @Test
  void testGetSession_ServiceUnavailable_503() throws Exception {
    when(sessionService.getSession(any())).thenThrow(
        new QodApiException(HttpStatus.SERVICE_UNAVAILABLE,
            "The service is currently not available"));
    mockMvc.perform(MockMvcRequestBuilders
            .get(SESSION_URI + "/" + SESSION_UUID)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message")
            .value("The service is currently not available"));
  }

  /* delete session */

  @Test
  void testDeleteSession_Ok_204() throws Exception {
    when(sessionService.deleteSession(any())).thenReturn(createSessionInfoSample());
    mockMvc.perform(MockMvcRequestBuilders
            .delete(SESSION_URI + "/" + SESSION_UUID)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isNoContent());
  }

  @Test
  void testDeleteSession_NotFound_404() throws Exception {
    when(sessionService.deleteSession(any())).thenThrow(
        new QodApiException(HttpStatus.NOT_FOUND,
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

  @Test
  void testDeleteSession_ServiceUnavailable_503() throws Exception {
    when(sessionService.deleteSession(any())).thenThrow(
        new QodApiException(HttpStatus.SERVICE_UNAVAILABLE,
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
  void testExtendQosSessionDuration_Ok_200() throws Exception {
    when(sessionService.extendQosSession(any(), any())).thenReturn(createSessionInfoSample());
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI + "/" + SESSION_UUID + "/extend")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("""
                {
                  "requestedAdditionalDuration": 60
                }"""))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.webhook.notificationAuthToken").value("c8974e592c2fa383d4a3960714"))
        .andExpect(jsonPath("$.sessionId").value(SESSION_UUID))
        .andExpect(jsonPath("$.duration").value(60));
  }

  @Test
  void testExtendQosSessionDuration_NotFound_404() throws Exception {
    when(sessionService.extendQosSession(any(), any())).thenThrow(
        new QodApiException(HttpStatus.NOT_FOUND,
            "QoD session not found for session ID: 963ab9f5-26e8-48b9-a56e-52ecdeaa9172"));
    mockMvc.perform(MockMvcRequestBuilders
            .post(SESSION_URI + "/" + SESSION_UUID + "/extend")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("""
                {
                  "requestedAdditionalDuration": 60
                }"""))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message")
            .value("QoD session not found for session ID: 963ab9f5-26e8-48b9-a56e-52ecdeaa9172"));
  }
}
