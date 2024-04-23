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

package com.camara.qod.service;

import static com.camara.qod.util.SessionsTestData.getH2QosSessionTestData;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camara.network.api.model.UserPlaneEvent;
import com.camara.qod.api.model.QosStatus;
import com.camara.qod.config.NetworkConfig;
import com.camara.qod.config.QodConfig;
import com.camara.qod.entity.H2QosSession;
import com.camara.qod.repository.QodSessionH2Repository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class
})
@ActiveProfiles(profiles = "local")
class NotificationServiceTest {

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private QodSessionH2Repository h2Repository;

  @MockBean
  private EventHubService eventHubService;

  @Autowired
  NetworkConfig networkConfig;

  @Autowired
  QodConfig qodConfig;

  private UUID savedSessionId;
  private String savedSubscriptionId;

  @SneakyThrows
  @BeforeEach
  public void setUpTest() {
    when(eventHubService.sendEvent(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    H2QosSession savedSession = h2Repository.save(getH2QosSessionTestData());
    assertEquals(QosStatus.REQUESTED, savedSession.getQosStatus());
    savedSessionId = savedSession.getId();
    savedSubscriptionId = savedSession.getSubscriptionId();
  }

  @AfterEach
  public void tearDown() {
    h2Repository.deleteById(savedSessionId);
  }

  @Test
  void testHandleQosNotification_SuccessfulResourceAllocation() {
    assertDoesNotThrow(
        () -> notificationService.handleQosNotification(savedSubscriptionId, UserPlaneEvent.SUCCESSFUL_RESOURCES_ALLOCATION));
    H2QosSession entity = h2Repository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(entity);
    assertEquals(QosStatus.AVAILABLE, entity.getQosStatus());
    verify(eventHubService, times(1)).sendEvent(any());
  }

  @ParameterizedTest
  @EnumSource(names = {"SESSION_TERMINATION", "FAILED_RESOURCES_ALLOCATION"})
  void testHandleQosNotification_DeletionDelay(UserPlaneEvent event) {
    long deletionDelay = 2;
    qodConfig.setDeletionDelay(deletionDelay);
    long now = Instant.now().getEpochSecond();
    H2QosSession entity = h2Repository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(entity);
    //Current remaining time of the session
    assertTrue(now - entity.getExpiresAt() > deletionDelay);

    assertDoesNotThrow(() -> notificationService.handleQosNotification(savedSubscriptionId, event));

    entity = h2Repository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(entity);
    //Remaining time was reduced to 2
    assertTrue(now - entity.getExpiresAt() <= deletionDelay);
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testHandleQosNotification_Ok_NoSubscriptionId(CapturedOutput output) {
    assertDoesNotThrow(() -> notificationService.handleQosNotification("NotFoundId", UserPlaneEvent.SESSION_TERMINATION));
    assertTrue(output.getAll().contains("Callback Subscription-ID <NotFoundId> does not have a corresponding existing QoD-Session"));
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testHandleQosNotification_UnhandledEvent(CapturedOutput output) {
    UserPlaneEvent event = UserPlaneEvent.QOS_NOT_GUARANTEED;
    assertDoesNotThrow(() -> notificationService.handleQosNotification(savedSubscriptionId, event));
    assertTrue(output.getAll().contains("Unhandled Notification Event <" + event + ">"));
  }

}
