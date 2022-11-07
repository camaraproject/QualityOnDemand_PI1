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

package com.camara.qod.service;

import static com.camara.qod.util.SessionsTestData.SESSION_UUID;
import static com.camara.qod.util.SessionsTestData.createSessionInfoSample;
import static com.camara.qod.util.SessionsTestData.createTestSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.camara.datatypes.model.QosSession;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.Notification;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.api.model.UeId;
import com.camara.qod.api.notifications.SessionNotificationsCallbackApi;
import com.camara.qod.config.QodConfig;
import com.camara.qod.config.ScefConfig;
import com.camara.qod.controller.SessionApiException;
import com.camara.qod.mapping.ModelMapper;
import com.camara.qod.plugin.storage.RedisConfig;
import com.camara.qod.plugin.storage.model.RedisQosSession;
import com.camara.qod.plugin.storage.repository.QodSessionRepository;
import com.camara.scef.api.ApiClient;
import com.camara.scef.api.model.UserPlaneEvent;
import com.qod.service.BookkeeperService;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import redis.embedded.RedisServer;

@Slf4j
@SpringBootTest
@ActiveProfiles(profiles = "test")
class QodServiceImplTest {

  private static RedisServer redisServer = null;

  @MockBean
  QodSessionRepository sessionRepo;

  @MockBean
  SessionNotificationsCallbackApi notificationsCallbackApi;

  @MockBean
  ModelMapper modelMapper;

  @MockBean
  BookkeeperService bookkeeperService;

  @MockBean
  @Qualifier("com.camara.scef.api.ApiClient")
  ApiClient apiClient;

  @Autowired
  QodService service;

  @Autowired
  ScefConfig scefConfig;

  @Autowired
  QodConfig qodConfig;

  @Autowired
  RedisConfig redisConfig;

  @BeforeAll
  public static void setUp() {
    redisServer = new RedisServer(6370);
    if (redisServer.isActive()) {
      redisServer.stop();
    }
    redisServer.start();
  }

  @AfterAll
  public static void tearDown() {
    redisServer.stop();
  }

  @Test
  void handleQosNotification_ok_NoSubscriptionId() throws ExecutionException, InterruptedException {
    when(sessionRepo.findBySubscriptionId(anyString())).thenReturn(Optional.empty());
    final CompletableFuture<Void> result = service.handleQosNotification("123", UserPlaneEvent.SESSION_TERMINATION);
    await().atMost(10, TimeUnit.SECONDS).until(result::isDone);
    assertThat(result).isCompleted();
    assertThat(result.get()).isNull();
  }

  @Test
  void handleQosNotification_ok_NotificationUriIsNull() throws Exception {
    when(sessionRepo.findBySubscriptionId(anyString())).thenReturn(
        Optional.of(RedisQosSession.builder().id(UUID.fromString(SESSION_UUID)).build()));
    when(sessionRepo.findById(UUID.fromString(SESSION_UUID))).thenReturn(Optional.of(RedisQosSession.builder().build()));
    SessionInfo info = createSessionInfoSample();
    info.setNotificationUri(null);
    when(modelMapper.map(any(QosSession.class))).thenReturn(info);
    final CompletableFuture<Void> result = service.handleQosNotification("123", UserPlaneEvent.SESSION_TERMINATION);
    await().atMost(10, TimeUnit.SECONDS).until(result::isDone);
    assertThat(result).isCompleted();
    assertThat(result.get()).isNull();
  }

  @Test
  void handleQosNotification_UnknownHost() throws Exception {
    when(sessionRepo.findBySubscriptionId(anyString())).thenReturn(
        Optional.of(RedisQosSession.builder().id(UUID.fromString(SESSION_UUID)).build()));
    when(sessionRepo.findById(UUID.fromString(SESSION_UUID))).thenReturn(Optional.of(RedisQosSession.builder().build()));
    when(modelMapper.map(any(QosSession.class))).thenReturn(createSessionInfoSample());
    String errorMessage = "I/O error on POST request for \"https://application-server.com/notifications/notifications\": "
        + "application-server.com; nested exception is java.net.UnknownHostException: application-server.com";
    doThrow(new ResourceAccessException(errorMessage)).when(notificationsCallbackApi).postNotification(any(Notification.class));

    final CompletableFuture<Void> result = service.handleQosNotification("123", UserPlaneEvent.SESSION_TERMINATION);
    await().atMost(10, TimeUnit.SECONDS).until(result::isDone);
    assertThat(result)
        .isCompletedExceptionally()
        .failsWithin(Duration.ZERO)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(ResourceAccessException.class)
        .withMessageContaining(errorMessage);
  }

  @Test
  void handleQosNotification_NotFound() {
    when(sessionRepo.findBySubscriptionId(anyString())).thenReturn(
        Optional.of(RedisQosSession.builder().id(UUID.fromString(SESSION_UUID)).build()));

    final CompletableFuture<Void> result = service.handleQosNotification("123", UserPlaneEvent.SESSION_TERMINATION);
    await().atMost(10, TimeUnit.SECONDS).until(result::isDone);

    assertThat(result)
        .isCompletedExceptionally()
        .failsWithin(Duration.ZERO)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(SessionApiException.class)
        .withMessageContaining("QoD session not found for session ID: 000ab9f5-26e8-48b9-a56e-52ecdeaa9172");
  }

  @Test
  void createSession_Fail_QosAllowMultipleUeAddr() {
    ReflectionTestUtils.setField(qodConfig, "qosAllowMultipleUeAddr", false);
    CreateSession session = createTestSession(QosProfile.E);
    session.ueId(new UeId().ipv4addr("72.24.11.4/17"));
    SessionApiException thrownException = assertThrows(SessionApiException.class,
        () -> service.createSession(session));
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, thrownException.getHttpStatus());
    Assertions.assertEquals(
        "A network segment for UeIdIpv4Addr is not allowed in the current configuration: 72.24.11.4/17 is not allowed, but 72.24.11.4 is "
            + "allowed.",
        thrownException.getMessage()
    );
  }

}
