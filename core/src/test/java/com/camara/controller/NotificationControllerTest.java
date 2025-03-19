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

import static com.camara.util.NotificationsTestData.NOTIFICATION_URI;
import static com.camara.util.NotificationsTestData.TEST_NOTIFICATION_REQUEST_EMPTY_TRANSACTION;
import static com.camara.util.NotificationsTestData.createTestNotificationRequest;
import static com.camara.util.TestData.getAsJsonFormat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camara.annotation.UnsecuredWebMvcTest;
import com.camara.exception.ExceptionHandlerAdvice;
import com.camara.network.api.model.UserPlaneEvent;
import com.camara.network.api.notifications.NotificationsApiController;
import com.camara.service.NotificationService;
import com.camara.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

  @MockitoBean
  private NotificationService notificationService;

  @MockitoBean
  private SessionService sessionService;

  @Autowired
  private MockMvc mockMvc;

  @ParameterizedTest
  @EnumSource(value = UserPlaneEvent.class)
  void testNotificationsPost_AllEvents_NoContent_204(UserPlaneEvent event) throws Exception {
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
    mockMvc.perform(MockMvcRequestBuilders
            .post(NOTIFICATION_URI)
            .accept(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(TEST_NOTIFICATION_REQUEST_EMPTY_TRANSACTION))
        .andDo(print())
        .andExpect(status().isNoContent());
  }
}
