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

import static com.camara.qod.util.SessionsTestData.createTestSessionInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.camara.qod.api.model.QosStatus;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.api.model.StatusInfo;
import com.camara.qod.exception.QodApiException;
import com.camara.qod.feign.EventHubClient;
import com.camara.qod.kafka.CloudEventProducer;
import feign.FeignException;
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
        "cloudEventSourceUrl", "http://localhost:9091/qod/v0/sessions", true);
    FieldUtils.writeField(eventHubService, "isEventhubHorizonConfigured", false, true);
  }

  @Test
  void testSendEvent_Kafka_MissingWebhookUrl() {
    SessionInfo sessionInfo = createTestSessionInfo(UUID.randomUUID());
    sessionInfo.webhook(null);
    eventHubService.sendEvent(sessionInfo);
    verify(cloudEventProducer, times(0)).sendEvent(any());
  }

  @ParameterizedTest
  @EnumSource(names = {"AVAILABLE", "UNAVAILABLE"})
  void testSendEvent_Kafka_WithStatusInfo(QosStatus qosStatus) {
    SessionInfo sessionInfo = createTestSessionInfo();
    sessionInfo.setQosStatus(qosStatus);
    eventHubService.sendEvent(sessionInfo, StatusInfo.NETWORK_TERMINATED);
    verify(cloudEventProducer, times(1)).sendEvent(any());
  }

  @Test
  @SneakyThrows
  void testSendEvent_Horizon_WithStatusInfo() {
    FieldUtils.writeField(eventHubService, "isEventhubHorizonConfigured", true, true);
    SessionInfo sessionInfo = createTestSessionInfo();
    eventHubService.sendEvent(sessionInfo, StatusInfo.NETWORK_TERMINATED);
    verify(eventHubClient, times(1)).sendEvent(any());
  }

  @Test
  @SneakyThrows
  void testSendEvent_Horizon_FeignException() {
    doThrow(FeignException.class)
        .when(eventHubClient).sendEvent(any());
    FieldUtils.writeField(eventHubService, "isEventhubHorizonConfigured", true, true);
    SessionInfo sessionInfo = createTestSessionInfo();
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> eventHubService.sendEvent(sessionInfo, StatusInfo.NETWORK_TERMINATED));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, qodApiException.getHttpStatus());
    assertEquals("The eventhub service is currently not available", qodApiException.getMessage());
    verify(eventHubClient, times(1)).sendEvent(any());
  }
}
