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

import static com.camara.util.SessionsTestData.createTestSessionInfo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.camara.exception.QodApiException;
import com.camara.feign.EventHubClient;
import com.camara.kafka.CloudEventProducer;
import com.camara.quality_on_demand.api.model.AccessTokenCredential;
import com.camara.quality_on_demand.api.model.QosStatus;
import com.camara.quality_on_demand.api.model.SessionInfo;
import com.camara.quality_on_demand.api.model.StatusInfo;
import feign.FeignException;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EventHubServiceTest {

  @InjectMocks
  private EventHubService eventHubService;

  @Mock
  private EventHubClient eventHubClient;

  @Mock
  private CloudEventProducer cloudEventProducer;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    FieldUtils.writeField(eventHubService,
        "cloudEventSourceUrl", "http://localhost:9091/quality-on-demand/v0.11/sessions", true);
    FieldUtils.writeField(eventHubService, "isEventhubHorizonConfigured", false, true);
  }

  @Test
  void testSendEvent_Kafka_MissingSinkUrl() {
    SessionInfo sessionInfo = createTestSessionInfo(UUID.randomUUID());
    sessionInfo.sink(null);
    eventHubService.sendEvent(sessionInfo);
    verify(cloudEventProducer, times(0)).sendEvent(any());
  }

  @ParameterizedTest
  @EnumSource(names = {"AVAILABLE", "UNAVAILABLE"})
  void testSendEvent_Kafka_WithStatusInfo(QosStatus qosStatus) {
    SessionInfo sessionInfo = createTestSessionInfo();
    sessionInfo.setQosStatus(qosStatus);
    sessionInfo.statusInfo(StatusInfo.NETWORK_TERMINATED);
    assertDoesNotThrow(() -> eventHubService.sendEvent(sessionInfo));
    verify(cloudEventProducer, times(1)).sendEvent(any());
  }

  @ParameterizedTest
  @EnumSource(names = {"AVAILABLE", "UNAVAILABLE"})
  void testSendEventWithSinkCredential_Kafka_WithStatusInfo(QosStatus qosStatus) {
    SessionInfo sessionInfo = createTestSessionInfo();
    sessionInfo.setQosStatus(qosStatus);
    sessionInfo.statusInfo(StatusInfo.NETWORK_TERMINATED);
    var accessTokenCredential = new AccessTokenCredential()
        .accessToken("123ABC")
        .accessTokenExpiresUtc(OffsetDateTime.now());
    sessionInfo.setSinkCredential(accessTokenCredential);
    assertDoesNotThrow(() -> eventHubService.sendEvent(sessionInfo));
    verify(cloudEventProducer, times(1)).sendEventWithAuthorization(any(), any());
  }

  @Test
  @SneakyThrows
  void testSendEvent_Horizon_WithStatusInfo() {
    FieldUtils.writeField(eventHubService, "isEventhubHorizonConfigured", true, true);
    SessionInfo sessionInfo = createTestSessionInfo();
    sessionInfo.statusInfo(StatusInfo.NETWORK_TERMINATED);
    eventHubService.sendEvent(sessionInfo);
    verify(eventHubClient, times(1)).sendEvent(any());
  }

  @Test
  @SneakyThrows
  void testSendEvent_Horizon_FeignException() {
    doThrow(FeignException.class)
        .when(eventHubClient).sendEvent(any());
    FieldUtils.writeField(eventHubService, "isEventhubHorizonConfigured", true, true);
    SessionInfo sessionInfo = createTestSessionInfo();
    sessionInfo.statusInfo(StatusInfo.NETWORK_TERMINATED);
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> eventHubService.sendEvent(sessionInfo));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, qodApiException.getHttpStatus());
    assertEquals("The eventhub service is currently not available", qodApiException.getMessage());
    verify(eventHubClient, times(1)).sendEvent(any());
  }
}
