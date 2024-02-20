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
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import com.camara.network.api.AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi;
import com.camara.network.api.model.UserPlaneEvent;
import com.camara.qod.api.model.QosStatus;
import com.camara.qod.config.NetworkConfig;
import com.camara.qod.config.QodConfig;
import com.camara.qod.entity.H2QosSession;
import com.camara.qod.exception.EventProducerException;
import com.camara.qod.repository.QodSessionH2Repository;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;

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

  @MockBean
  AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi deleteOperationApi;

  @Autowired
  NetworkConfig networkConfig;

  @Autowired
  QodConfig qodConfig;

  private UUID savedSessionId;
  private String savedSubscriptionId;

  @SneakyThrows
  @BeforeEach
  public void setUpTest() {
    doNothing().when(eventHubService).sendEvent(any(), any());
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
  void testHandleQosNotification_Ok_NoSubscriptionId() throws ExecutionException, InterruptedException {
    final CompletableFuture<Void> result = notificationService.handleQosNotification("NotFoundId", UserPlaneEvent.SESSION_TERMINATION);
    await().atMost(10, TimeUnit.SECONDS).until(result::isDone);
    assertThat(result).isCompleted();
    assertThat(result.get()).isNull();
  }

  @Test
  void testHandleQosNotification_Ok_NotificationUrlIsNull() {
    String errorMessage = "Notification aborted! No notificationUrl was given in the qosSession: xxx";
    doThrow(new EventProducerException(errorMessage)).when(eventHubService).sendEvent(any(), any());

    H2QosSession savedSession = h2Repository.findById(savedSessionId).orElse(null);
    assertNotNull(savedSession);
    savedSession.setNotificationUrl(null);

    h2Repository.save(savedSession);

    final CompletableFuture<Void> result =
        notificationService.handleQosNotification(savedSession.getSubscriptionId(), UserPlaneEvent.SESSION_TERMINATION);
    await().atMost(10, TimeUnit.SECONDS).until(result::isDone);
    assertThat(result)
        .isCompletedExceptionally()
        .failsWithin(Duration.ZERO)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(EventProducerException.class)
        .withMessageContaining("");
  }

  @Test
  void testHandleQosNotification_UnknownHost() {
    String errorMessage = "I/O error on POST request for \"https://application-server.com/notifications/notifications\": "
        + "application-server.com; nested exception is java.net.UnknownHostException: application-server.com";
    doThrow(new ResourceAccessException(errorMessage)).when(eventHubService).sendEvent(any(), any());

    final CompletableFuture<Void> result =
        notificationService.handleQosNotification(savedSubscriptionId, UserPlaneEvent.SESSION_TERMINATION);
    await().atMost(10, TimeUnit.SECONDS).until(result::isDone);
    assertThat(result)
        .isCompletedExceptionally()
        .failsWithin(Duration.ZERO)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(ResourceAccessException.class)
        .withMessageContaining(errorMessage);
  }

  @Test
  void testHandleQosNotification_CompleteFlow() throws Exception {
    final CompletableFuture<Void> result = notificationService.handleQosNotification(savedSubscriptionId,
        UserPlaneEvent.SESSION_TERMINATION);
    await().atMost(10, TimeUnit.SECONDS).until(result::isDone);
    assertThat(result).isCompleted();
    assertThat(result.get()).isNull();
  }

  @Test
  void testHandleQosNotification_NotFound() {
    assertDoesNotThrow(() -> notificationService.handleQosNotification("123", UserPlaneEvent.SESSION_TERMINATION));
  }

  @ParameterizedTest
  @ValueSource(strings = {"AVAILABLE", "UNAVAILABLE"})
  void testSetQosStatusForSession_StatusUpdated(String qosStatusString) {
    QosStatus updatedStatus = QosStatus.valueOf(qosStatusString);
    notificationService.setQosStatusForSession(savedSubscriptionId, updatedStatus);
    H2QosSession updatedSession = h2Repository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(updatedSession);
    assertEquals(updatedStatus, updatedSession.getQosStatus());
  }

  @Test
  void testSetQosStatusForSession_SessionNotFound() {
    String notFoundId = "notFoundId";
    assertDoesNotThrow(() -> notificationService.setQosStatusForSession(notFoundId, QosStatus.AVAILABLE));
    assertFalse(h2Repository.findBySubscriptionId(notFoundId).isPresent());
  }

  @Test
  void testSetQosStatusForSession_StatusNotUpdated_EqualToExistingStatus() {
    assertDoesNotThrow(() -> notificationService.setQosStatusForSession(savedSubscriptionId, QosStatus.REQUESTED));
  }

}
