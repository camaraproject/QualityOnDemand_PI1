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

package com.camara.controller;

import static com.camara.util.SessionsTestData.QOD_SESSIONS_RETRIEVE_URI;
import static com.camara.util.SessionsTestData.QOD_SESSIONS_URI;
import static com.camara.util.SessionsTestData.SESSION_UUID;
import static com.camara.util.SessionsTestData.createSessionInfoSample;
import static com.camara.util.SessionsTestData.createTestSession;
import static com.camara.util.SessionsTestData.createValidTestSession;
import static com.camara.util.TestData.getAsJsonFormat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camara.annotation.UnsecuredWebMvcTest;
import com.camara.exception.ExceptionHandlerAdvice;
import com.camara.exception.QodApiException;
import com.camara.quality_on_demand.api.QoSSessionsApiController;
import com.camara.quality_on_demand.api.model.Device;
import com.camara.quality_on_demand.api.model.ExtendSessionDuration;
import com.camara.quality_on_demand.api.model.RetrieveSessionsInput;
import com.camara.security.SecurityStandardConfig;
import com.camara.service.SessionService;
import com.camara.service.ValidationService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@UnsecuredWebMvcTest(controllers = QoSSessionsApiController.class)
@Import({SessionsController.class, ExceptionHandlerAdvice.class, SecurityStandardConfig.class})
@ContextConfiguration(classes = QoSSessionsApiController.class)
class SessionsControllerTest {

  @MockitoBean
  private SessionService sessionService;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ValidationService validationService;

  @Nested
  class CreateSessionTests {

    @Test
    void testCreateSession_Created_201() throws Exception {
      when(sessionService.createSession(any(), anyBoolean())).thenReturn(createSessionInfoSample());
      mockMvc.perform(post(QOD_SESSIONS_URI)
              .accept(MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .content(getAsJsonFormat(createValidTestSession())))
          .andDo(MockMvcResultHandlers.print())
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.sessionId").value(SESSION_UUID));
      verify(sessionService, times(1)).createSession(any(), eq(true));
    }

    @Test
    void testCreateSession_Created_WithoutDeviceInRequest_201() throws Exception {
      when(sessionService.createSession(any(), anyBoolean())).thenReturn(createSessionInfoSample().device(null));
      mockMvc.perform(post(QOD_SESSIONS_URI)
              .accept(MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .content(getAsJsonFormat(createValidTestSession().device(null))))
          .andDo(MockMvcResultHandlers.print())
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.sessionId").value(SESSION_UUID))
          .andExpect(jsonPath("$.device").doesNotExist());
      verify(sessionService, times(1)).createSession(any(), eq(false));
    }

    @Test
    void testCreateSession_BadRequest_InvalidDuration_400() throws Exception {
      mockMvc.perform(post(QOD_SESSIONS_URI)
              .accept(MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .content(getAsJsonFormat(createTestSession(0))))
          .andDo(MockMvcResultHandlers.print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Validation failed for parameter 'duration'"));
    }

    @Test
    void testCreateSession_MethodNotAllowed_405() throws Exception {
      mockMvc.perform(get(QOD_SESSIONS_URI)
              .accept(MediaType.APPLICATION_JSON)
              .contentType(MediaType.APPLICATION_JSON_VALUE))
          .andDo(MockMvcResultHandlers.print())
          .andExpect(status().isMethodNotAllowed());
    }
  }

  @Nested
  class DeleteSessionTests {

    @Test
    void testDeleteSession_Ok_204() throws Exception {
      mockMvc.perform(delete(QOD_SESSIONS_URI + "/" + SESSION_UUID)
              .accept(MediaType.APPLICATION_JSON_VALUE))
          .andDo(MockMvcResultHandlers.print())
          .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteSession_NotFound_404() throws Exception {
      doThrow(new QodApiException(HttpStatus.NOT_FOUND, "Session not found"))
          .when(sessionService).getSessionInfoById(any(UUID.class));

      mockMvc.perform(delete(QOD_SESSIONS_URI + "/" + SESSION_UUID)
              .accept(MediaType.APPLICATION_JSON_VALUE))
          .andDo(MockMvcResultHandlers.print())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Session not found"));
    }
  }

  @Nested
  class GetSessionTests {

    @Test
    void testGetSession_Ok_200() throws Exception {
      when(sessionService.getSessionInfoById(any(UUID.class))).thenReturn(createSessionInfoSample());
      mockMvc.perform(get(QOD_SESSIONS_URI + "/" + SESSION_UUID)
              .accept(MediaType.APPLICATION_JSON_VALUE))
          .andDo(MockMvcResultHandlers.print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.sessionId").value(SESSION_UUID));
    }
  }

  @Nested
  class ExtendSessionTests {

    @Test
    void testExtendQosSessionDuration_Ok_200() throws Exception {
      when(sessionService.extendQosSession(any(), any())).thenReturn(createSessionInfoSample());

      var request = new ExtendSessionDuration().requestedAdditionalDuration(60);
      mockMvc.perform(post(QOD_SESSIONS_URI + "/" + SESSION_UUID + "/extend")
              .accept(MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .content(getAsJsonFormat(request)))
          .andDo(MockMvcResultHandlers.print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.sessionId").value(SESSION_UUID))
          .andExpect(jsonPath("$.duration").value(60));
    }
  }

  @Nested
  class RetrieveSessionsByDeviceTests {

    @Test
    void testRetrieveSessionsByDevice_Ok_200() throws Exception {
      when(sessionService.getSessionsByDevice(any())).thenReturn(List.of(createSessionInfoSample()));

      var input = new RetrieveSessionsInput().device(new Device());
      mockMvc.perform(post(QOD_SESSIONS_RETRIEVE_URI)
              .accept(MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .content(getAsJsonFormat(input)))
          .andDo(MockMvcResultHandlers.print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].sessionId").value(SESSION_UUID));
    }

    @Test
    void testRetrieveSessionsByDevice_EmptyList_200() throws Exception {
      when(sessionService.getSessionsByDevice(any())).thenReturn(Collections.emptyList());

      var input = new RetrieveSessionsInput().device(new Device());
      mockMvc.perform(post(QOD_SESSIONS_RETRIEVE_URI)
              .accept(MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .content(getAsJsonFormat(input)))
          .andDo(MockMvcResultHandlers.print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }
}
