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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camara.network.api.AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi;
import com.camara.qod.api.model.QosStatus;
import com.camara.qod.api.model.StatusInfo;
import com.camara.qod.entity.H2QosSession;
import com.camara.qod.repository.QodSessionH2Repository;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class SessionServiceTest {

  @Autowired
  private SessionService sessionService;

  @Autowired
  private QodSessionH2Repository h2Repository;

  @MockBean
  private EventHubService eventHubService;

  @MockBean
  private AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi deleteApi;

  private UUID savedSessionId;
  private String savedSubscriptionId;

  @SneakyThrows
  @BeforeEach
  public void setUpTest() {
    when(eventHubService.sendEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
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
  void testDeleteAndNotify() {
    assertDoesNotThrow(() -> sessionService.deleteAndNotify(savedSessionId, StatusInfo.NETWORK_TERMINATED));
    verify(eventHubService, times(1)).sendEvent(any(), any());
  }

  @ParameterizedTest
  @EnumSource(names = {"AVAILABLE", "UNAVAILABLE"})
  void testDeleteAndNotify_DeleteRequested(QosStatus qosStatus) {
    H2QosSession h2QosSession = h2Repository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(h2QosSession);
    h2QosSession.setQosStatus(qosStatus);
    h2Repository.saveAndFlush(h2QosSession);
    assertDoesNotThrow(() -> sessionService.deleteAndNotify(savedSessionId, StatusInfo.DELETE_REQUESTED));
    if (qosStatus == QosStatus.AVAILABLE) {
      verify(eventHubService, times(1)).sendEvent(any(), any());
    } else {
      verify(eventHubService, times(0)).sendEvent(any(), any());
    }
  }
}
