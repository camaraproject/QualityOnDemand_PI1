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

import static com.camara.qod.util.SessionsTestData.NOTIFICATION_URI;
import static com.camara.qod.util.SessionsTestData.getNotificationRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camara.qod.service.QodService;
import com.camara.scef.api.notifications.NotificationsApiController;
import java.util.concurrent.CompletableFuture;
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

@WebMvcTest(NotificationsApiController.class)
@Import(value = {
    NotificationsController.class,
    ExceptionHandlerAdvice.class
})
@ContextConfiguration(classes = NotificationsApiController.class)
class NotificationControllerTest {

  @MockBean
  QodService qodService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void notificationsPost_ok() throws Exception {
    when(qodService.handleQosNotification(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

    mockMvc.perform(MockMvcRequestBuilders
            .post(NOTIFICATION_URI)
            .accept(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getNotificationRequest())) // UserPlaneNotificationData
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isNoContent());
  }

  @Test
  void notificationsPost_NotFound() throws Exception {
    doThrow(new SessionApiException(HttpStatus.NOT_FOUND,
        "QoD session not found for session ID: 963ab9f5-26e8-48b9-a56e-52ecdeaa9172")).when(
        qodService).handleQosNotification(anyString(), any());

    mockMvc.perform(MockMvcRequestBuilders
            .post(NOTIFICATION_URI)
            .accept(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getNotificationRequest())) // UserPlaneNotificationData
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.message")
            .value("QoD session not found for session ID: 963ab9f5-26e8-48b9-a56e-52ecdeaa9172"));
  }

  @Test
  void notificationsPost_ServiceUnavailable() throws Exception {
    doThrow(new SessionApiException(HttpStatus.SERVICE_UNAVAILABLE,
        "The service is currently not available")).when(
        qodService).handleQosNotification(anyString(), any());

    mockMvc.perform(MockMvcRequestBuilders
            .post(NOTIFICATION_URI)
            .accept(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getNotificationRequest())) // UserPlaneNotificationData
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error.message")
            .value("The service is currently not available"));
  }
}
