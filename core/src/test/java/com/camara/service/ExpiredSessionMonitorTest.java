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

import static com.camara.util.SessionsTestData.SESSION_UUID;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camara.entity.QosSession;
import com.camara.quality_on_demand.api.model.QosStatus;
import com.camara.quality_on_demand.api.model.SessionInfo;
import com.camara.quality_on_demand.api.model.StatusInfo;
import com.camara.repository.QosProfileRepository;
import com.camara.repository.QosSessionRepository;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "qod.expiration.trigger-interval=1")
@DirtiesContext
class ExpiredSessionMonitorTest {

  private QosSession expiringSession;

  @MockitoBean
  private EventHubService eventHubService;

  @MockitoBean
  private QosSessionRepository qosSessionRepository;

  @MockitoBean
  private QosProfileRepository qosProfileRepository;

  @MockitoBean
  private NetworkService networkService;

  @MockitoBean
  private LockProvider lockProvider;

  @Mock
  private SimpleLock lock;

  @BeforeEach
  public void setUp() {
    expiringSession = new QosSession();
    expiringSession.setSessionId(SESSION_UUID);
    expiringSession.setExpiresAt(OffsetDateTime.now().plusSeconds(1).format(ISO_DATE_TIME));
    expiringSession.setQosStatus(QosStatus.AVAILABLE);

    when(qosSessionRepository.findBySessionId(any())).thenReturn(Optional.of(expiringSession));
    when(qosSessionRepository.findAll()).thenReturn(List.of(expiringSession));
    when(lockProvider.lock(any())).thenReturn(Optional.of(lock));
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testExpiringSession(CapturedOutput output) {
    Awaitility.await().atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(qosSessionRepository, times(1)).deleteBySessionId(any()));
    assertTrue(output.getAll().contains("QoS sessions which will soon expire: [" + SESSION_UUID + "]"));
  }

  @Test
  void testExpiringSession_SessionIsAlreadyMarkedForDeletion() {
    expiringSession.setScheduledForDeletion(true);
    when(qosSessionRepository.findAll()).thenReturn(List.of(expiringSession));
    Awaitility.await().atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(qosSessionRepository, times(0)).deleteBySessionId(any()));
    expiringSession.setScheduledForDeletion(false);
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testExpiringSession_NothingExpiresSoon(CapturedOutput output) {
    when(qosSessionRepository.findAll()).thenReturn(Collections.emptyList());

    Awaitility.await().atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(qosSessionRepository, times(0)).deleteBySessionId(any()));
    assertFalse(output.getAll().contains("QoS sessions which will soon expire"));
  }

  @ParameterizedTest
  @EnumSource(names = {"AVAILABLE", "REQUESTED"})
  void testExpiringSession_DurationExpiration(QosStatus qosStatus) {
    expiringSession.setQosStatus(qosStatus);
    when(qosSessionRepository.findAll()).thenReturn(List.of(expiringSession));

    Awaitility.await().atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(qosSessionRepository, times(1)).deleteBySessionId(any()));

    ArgumentCaptor<SessionInfo> sessionInfoCaptor = ArgumentCaptor.forClass(SessionInfo.class);
    verify(eventHubService, times(1)).sendEvent(sessionInfoCaptor.capture());

    SessionInfo capturedSessionInfo = sessionInfoCaptor.getValue();
    assertEquals(QosStatus.UNAVAILABLE, capturedSessionInfo.getQosStatus());
    assertEquals(StatusInfo.DURATION_EXPIRED, capturedSessionInfo.getStatusInfo());
  }

  @Test
  void testExpiringSession_NetworkTerminated() {
    expiringSession.setQosStatus(QosStatus.UNAVAILABLE);
    when(qosSessionRepository.findAll()).thenReturn(List.of(expiringSession));

    Awaitility.await().atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(qosSessionRepository, times(1)).deleteBySessionId(any()));

    ArgumentCaptor<SessionInfo> sessionInfoCaptor = ArgumentCaptor.forClass(SessionInfo.class);
    verify(eventHubService, times(1)).sendEvent(sessionInfoCaptor.capture());

    SessionInfo capturedSessionInfo = sessionInfoCaptor.getValue();
    assertEquals(QosStatus.UNAVAILABLE, capturedSessionInfo.getQosStatus());
    assertEquals(StatusInfo.NETWORK_TERMINATED, capturedSessionInfo.getStatusInfo());
  }
}
