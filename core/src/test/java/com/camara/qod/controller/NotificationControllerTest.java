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

import static com.camara.qod.util.NotificationsTestData.NOTIFICATION_URI;
import static com.camara.qod.util.NotificationsTestData.TEST_NOTIFICATION_REQUEST;
import static com.camara.qod.util.NotificationsTestData.TEST_NOTIFICATION_REQUEST_EMPTY_TRANSACTION;
import static com.camara.qod.util.NotificationsTestData.createTestNotificationRequest;
import static com.camara.qod.util.TestData.getAsJsonFormat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camara.network.api.model.UserPlaneEvent;
import com.camara.network.api.notifications.NotificationsApiController;
import com.camara.qod.annotation.UnsecuredWebMvcTest;
import com.camara.qod.exception.ExceptionHandlerAdvice;
import com.camara.qod.exception.QodApiException;
import com.camara.qod.service.NotificationService;
import com.camara.qod.service.SessionService;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@UnsecuredWebMvcTest(controllers = NotificationsApiController.class)
@Import(value = {
    NotificationsController.class,
    ExceptionHandlerAdvice.class
})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = NotificationsApiController.class)
class NotificationControllerTest {

  @MockBean
  private NotificationService notificationService;

  @MockBean
  private SessionService sessionService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void testNotificationsPost_NoContent_204() throws Exception {
    when(notificationService.handleQosNotification(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

    mockMvc.perform(MockMvcRequestBuilders
            .post(NOTIFICATION_URI)
            .accept(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(TEST_NOTIFICATION_REQUEST_EMPTY_TRANSACTION))
        .andDo(print())
        .andExpect(status().isNoContent());
  }

  @ParameterizedTest
  @EnumSource(value = UserPlaneEvent.class)
  void testNotificationsPost_AllEvents_NoContent_204(UserPlaneEvent event) throws Exception {
    when(notificationService.handleQosNotification(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

    mockMvc.perform(MockMvcRequestBuilders
            .post(NOTIFICATION_URI)
            .accept(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getAsJsonFormat(createTestNotificationRequest(event))))
        .andDo(print())
        .andExpect(status().isNoContent());
  }

  @Test
  void testNotificationsPost_NoContent_EmptyTransaction_204() throws Exception {
    when(notificationService.handleQosNotification(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

    mockMvc.perform(MockMvcRequestBuilders
            .post(NOTIFICATION_URI)
            .accept(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(TEST_NOTIFICATION_REQUEST_EMPTY_TRANSACTION))
        .andDo(print())
        .andExpect(status().isNoContent());
  }

  @Test
  void testNotificationsPost_NotFound_404() throws Exception {
    doThrow(new QodApiException(HttpStatus.NOT_FOUND,
        "QoD session not found for session ID: 963ab9f5-26e8-48b9-a56e-52ecdeaa9172")).when(
        notificationService).handleQosNotification(anyString(), any());

    mockMvc.perform(MockMvcRequestBuilders
            .post(NOTIFICATION_URI)
            .accept(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(TEST_NOTIFICATION_REQUEST))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message")
            .value("QoD session not found for session ID: 963ab9f5-26e8-48b9-a56e-52ecdeaa9172"));
  }

  @Test
  void testNotificationsPost_ServiceUnavailable_503() throws Exception {
    doThrow(new QodApiException(HttpStatus.SERVICE_UNAVAILABLE,
        "The service is currently not available")).when(
        notificationService).handleQosNotification(anyString(), any());

    mockMvc.perform(MockMvcRequestBuilders
            .post(NOTIFICATION_URI)
            .accept(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(TEST_NOTIFICATION_REQUEST))
        .andDo(print())
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message")
            .value("The service is currently not available"));
  }
}
