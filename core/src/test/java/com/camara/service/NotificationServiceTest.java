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

package com.camara.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camara.config.QodConfig;
import com.camara.entity.QosSession;
import com.camara.network.api.model.UserPlaneEvent;
import com.camara.quality_on_demand.api.model.QosStatus;
import com.camara.repository.QosProfileRepository;
import com.camara.repository.QosSessionRepository;
import com.camara.util.SessionsTestData;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DirtiesContext
@SpringBootTest(properties = "qod.expiration.trigger-interval=1")
@EnableAutoConfiguration(exclude = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class
})
@ActiveProfiles("test")
class NotificationServiceTest {

  @Autowired
  private QodConfig qodConfig;

  @Autowired
  private NotificationService notificationService;

  @MockitoBean
  private QosSessionRepository qosSessionRepository;

  @MockitoBean
  private QosProfileRepository qosProfileRepository;

  @MockitoBean
  private EventHubService eventHubService;

  @MockitoBean
  private ExpiredSessionMonitor expiredSessionMonitor;

  private String savedSubscriptionId;

  @SneakyThrows
  @BeforeEach
  public void setUpTest() {
    QosSession qosSessionTestData = SessionsTestData.createQosSessionTestData();
    when(eventHubService.sendEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
    when(qosSessionRepository.findBySubscriptionId(any())).thenReturn(Optional.of(qosSessionTestData));
    savedSubscriptionId = qosSessionTestData.getSubscriptionId();
  }

  @Test
  void testHandleQosNotification_SuccessfulResourcesAllocation() {
    assertDoesNotThrow(
        () -> notificationService.handleQosNotification(savedSubscriptionId, UserPlaneEvent.SUCCESSFUL_RESOURCES_ALLOCATION));
    QosSession entity = qosSessionRepository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(entity);
    assertEquals(QosStatus.AVAILABLE, entity.getQosStatus());
    assertNotNull(entity.getStartedAt());
    OffsetDateTime expiresAt = OffsetDateTime.parse(entity.getExpiresAt());
    assertEquals(expiresAt, OffsetDateTime.parse(entity.getStartedAt()).plusSeconds(entity.getDuration()));
    verify(eventHubService, times(1)).sendEvent(any());
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testHandleQosNotification_CurrentStatus_Unavailable_NoUpdate(CapturedOutput output) {
    QosSession entity = qosSessionRepository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(entity);
    final OffsetDateTime oldExpiration = OffsetDateTime.parse(entity.getExpiresAt());
    entity.setQosStatus(QosStatus.UNAVAILABLE);
    qosSessionRepository.save(entity);

    assertDoesNotThrow(
        () -> notificationService.handleQosNotification(savedSubscriptionId, UserPlaneEvent.SUCCESSFUL_RESOURCES_ALLOCATION));

    entity = qosSessionRepository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(entity);
    assertNull(entity.getStartedAt());
    OffsetDateTime expirationAfterCallback = OffsetDateTime.parse(entity.getExpiresAt());
    assertEquals(oldExpiration, expirationAfterCallback);
    verify(eventHubService, times(0)).sendEvent(any());

    assertTrue(output.getAll()
        .contains("Network reported with SUCCESSFUL_RESOURCES_ALLOCATION, but session is in status <UNAVAILABLE>. No update."));
  }

  @ParameterizedTest
  @EnumSource(names = {"SESSION_TERMINATION", "FAILED_RESOURCES_ALLOCATION"})
  void testHandleQosNotification_DeletionDelay(UserPlaneEvent event) {
    long deletionDelay = 10;
    qodConfig.setDeletionDelay(deletionDelay);
    var now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);
    QosSession entity = qosSessionRepository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(entity);
    entity.setExpiresAt(OffsetDateTime.now().plusDays(1).toString());
    qosSessionRepository.save(entity);
    //Current remaining time of the session
    long remainingSessionTime = OffsetDateTime.parse(entity.getExpiresAt()).toEpochSecond() - now.toEpochSecond();
    assertTrue(remainingSessionTime > deletionDelay);

    assertDoesNotThrow(() -> notificationService.handleQosNotification(savedSubscriptionId, event));

    entity = qosSessionRepository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(entity);
    remainingSessionTime = OffsetDateTime.parse(entity.getExpiresAt()).toEpochSecond() - now.toEpochSecond();
    //The Remaining time was reduced to 2
    assertTrue(remainingSessionTime <= deletionDelay);
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testHandleQosNotification_SessionAlreadyMarkedForDeletion(CapturedOutput output) {
    QosSession entity = qosSessionRepository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(entity);
    final var qosStatusBeforeCallback = entity.getQosStatus();
    entity.setScheduledForDeletion(true);
    qosSessionRepository.save(entity);

    assertDoesNotThrow(
        () -> notificationService.handleQosNotification(savedSubscriptionId, UserPlaneEvent.SUCCESSFUL_RESOURCES_ALLOCATION));
    assertTrue(output.getAll().contains("is already locked for deletion. Notification callback will be skipped"));
    var entityAfterCallback = qosSessionRepository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(entityAfterCallback);
    assertEquals(entityAfterCallback.getQosStatus(), qosStatusBeforeCallback);
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testHandleQosNotification_Ok_NoSubscriptionId(CapturedOutput output) {
    when(qosSessionRepository.findBySubscriptionId(any())).thenReturn(Optional.empty());
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
