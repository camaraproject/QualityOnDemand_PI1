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

import static com.camara.qod.util.SessionsTestData.createTestSession;
import static com.camara.qod.util.SessionsTestData.getH2QosSessionTestData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.QosStatus;
import com.camara.qod.config.NetworkConfig;
import com.camara.qod.entity.H2QosSession;
import com.camara.qod.mapping.StorageModelMapper;
import com.camara.qod.model.QosSession;
import com.camara.qod.model.QosSessionIdWithExpiration;
import com.camara.qod.repository.QodSessionH2Repository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

class H2StorageServiceTest {

  private AutoCloseable closeable;

  @InjectMocks
  private H2StorageService h2StorageService;

  @Mock
  private QodSessionH2Repository qodSessionH2Repository;

  @Mock
  private NetworkConfig networkConfig;

  @Spy
  private StorageModelMapper storageModelMapper = Mappers.getMapper(StorageModelMapper.class);

  @BeforeEach
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();
  }

  @Test
  void testSaveAndDeleteSession() {
    long now = Instant.now().getEpochSecond();
    int duration = 2;
    QosSession session = h2StorageService.saveSession(
        now,
        now + duration,
        UUID.randomUUID(),
        createTestSession(2),
        "subscrId123",
        UUID.randomUUID());
    h2StorageService.deleteSession(session.getSessionId());

    session = h2StorageService.saveSession(QosSession.builder().sessionId(UUID.randomUUID()).build());
    h2StorageService.deleteSession(session.getSessionId());

    verify(qodSessionH2Repository, times(2)).save(any(H2QosSession.class));
    verify(qodSessionH2Repository, times(2)).deleteById(any(UUID.class));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testSaveAndDeleteSession_NefSupportResourceAllocationEvent(boolean isResourceAllocationSupported) {
    when(networkConfig.isSupportedEventResourceAllocation()).thenReturn(isResourceAllocationSupported);

    long now = Instant.now().getEpochSecond();
    int duration = 2;
    QosSession session = h2StorageService.saveSession(
        now,
        now + duration,
        UUID.randomUUID(),
        createTestSession(duration),
        "subscrId123",
        UUID.randomUUID());
    if (isResourceAllocationSupported) {
      assertEquals(QosStatus.REQUESTED, session.getQosStatus());
    } else {
      assertEquals(QosStatus.AVAILABLE, session.getQosStatus());
    }

    h2StorageService.deleteSession(session.getSessionId());

    verify(qodSessionH2Repository, times(1)).save(any(H2QosSession.class));
    verify(qodSessionH2Repository, times(1)).deleteById(any(UUID.class));
  }

  @Test
  void testSaveAndDeleteSession_WithoutWebhook() {
    long now = Instant.now().getEpochSecond();
    int duration = 2;
    CreateSession testSession = createTestSession(duration);
    testSession.setWebhook(null);

    QosSession session = h2StorageService.saveSession(
        now,
        now + duration,
        UUID.randomUUID(),
        testSession,
        "subscrId123",
        UUID.randomUUID());

    assertNull(session.getNotificationUrl());
    assertNull(session.getNotificationAuthToken());
    h2StorageService.deleteSession(session.getSessionId());

    verify(qodSessionH2Repository, times(1)).save(any(H2QosSession.class));
    verify(qodSessionH2Repository, times(1)).deleteById(any(UUID.class));
  }

  @Test
  void testGetSession() throws Exception {
    when(qodSessionH2Repository.findById(any(UUID.class))).thenReturn(Optional.of(getH2QosSessionTestData()));
    assertTrue(h2StorageService.getSession(UUID.randomUUID()).isPresent());
  }

  @Test
  void testGetUnknownSession() {
    when(qodSessionH2Repository.findById(any(UUID.class))).thenReturn(Optional.empty());
    Optional<QosSession> session = h2StorageService.getSession(UUID.randomUUID());
    assertTrue(session.isEmpty());
    verify(qodSessionH2Repository, times(1)).findById(any());
  }

  @Test
  void testGetSessionsThatExpireUntil() throws Exception {
    when(qodSessionH2Repository.findByExpiresAtLessThan(anyLong()))
        .thenReturn(Collections.singletonList(getH2QosSessionTestData()));
    long now = Instant.now().getEpochSecond();
    List<QosSessionIdWithExpiration> sessionsThatExpireUntil = h2StorageService.getSessionsThatExpireUntil(now + 2);
    verify(qodSessionH2Repository, times(1)).findByExpiresAtLessThan(anyLong());
    assertEquals(1, sessionsThatExpireUntil.size());
  }

  @Test
  void testFindByDeviceIpv4Addr() {
    h2StorageService.findByDeviceIpv4addr("198.51.100.1");
    verify(qodSessionH2Repository, times(1)).findByDeviceIpv4addr(any());
  }

  @Test
  void testFindBySubscriptionId() {
    h2StorageService.findBySubscriptionId("anyId");
    verify(qodSessionH2Repository, times(1)).findBySubscriptionId(anyString());
  }

}
