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

package com.camara.qod.security;

import static com.camara.qod.util.NotificationsTestData.NOTIFICATION_URI;
import static com.camara.qod.util.NotificationsTestData.TEST_NOTIFICATION_REQUEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camara.qod.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "device-location.notifications.ip-filter.enabled=true"
})
@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class Ipv4AllowListFilterTest {

  @Autowired
  private MockMvc mockMvc;
  @MockBean
  private JwtDecoder jwtDecoder;
  @MockBean
  private NotificationService notificationService;

  @Test
  void testNotifications_AllowedIpv4_204() throws Exception {
    mockMvc.perform(post(NOTIFICATION_URI)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(TEST_NOTIFICATION_REQUEST)
        )
        .andDo(print())
        .andExpect(status().isNoContent());
    verify(notificationService, times(1)).handleQosNotification(anyString(), any());
  }

  @Test
  void testNotifications_ForbiddenIpv4_403() throws Exception {

    mockMvc.perform(post(NOTIFICATION_URI)
            .with(request -> {
              request.setRemoteAddr("10.0.0.1");
              return request;
            })
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(TEST_NOTIFICATION_REQUEST)
        )
        .andDo(print())
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied"));
    verify(notificationService, times(0)).handleQosNotification(anyString(), any());
  }
}
