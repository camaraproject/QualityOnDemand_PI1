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

import com.camara.exception.QodApiException;
import com.camara.feign.EventHubClient;
import com.camara.kafka.CloudEventProducer;
import com.camara.model.CloudEventData;
import com.camara.quality_on_demand.api.model.CloudEvent.TypeEnum;
import com.camara.quality_on_demand.api.model.EventQosStatus;
import com.camara.quality_on_demand.api.model.QosStatus;
import com.camara.quality_on_demand.api.model.SessionInfo;
import com.camara.quality_on_demand.api.model.SinkCredential;
import com.camara.quality_on_demand.api.model.StatusInfo;
import com.camara.util.CloudEventSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.FeignException;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class EventHubService {

  private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private final CloudEventProducer cloudEventProducer;

  @Value("${qod.eventhub.horizon}")
  private boolean isEventhubHorizonConfigured;

  private final EventHubClient eventHubClient;

  @Value("${qod.cloud-event.source.url}")
  private String cloudEventSourceUrl;

  /**
   * Sends an event with {@link StatusInfo}.
   *
   * @param sessionInfo {@link SessionInfo}
   */
  @SneakyThrows
  @Async
  public CompletableFuture<Void> sendEvent(SessionInfo sessionInfo) {
    CompletableFuture<Void> completableFuture = CompletableFuture.completedFuture(null);
    if (isSinkMissing(sessionInfo)) {
      return completableFuture;
    }
    var cloudEvent = buildCloudEvent(sessionInfo);
    logCloudEvent(cloudEvent);
    if (isEventhubHorizonConfigured) {
      sendCloudEventHorizon(cloudEvent);
    } else {
      sendCloudEventKafka(cloudEvent, sessionInfo.getSinkCredential());
    }
    return completableFuture;
  }

  private static boolean isSinkMissing(SessionInfo sessionInfo) {
    var sink = sessionInfo.getSink();
    return StringUtils.isEmpty(sink);
  }

  private void sendCloudEventKafka(CloudEvent cloudEvent, SinkCredential sinkCredential) {
    if (sinkCredential != null) {
      cloudEventProducer.sendEventWithAuthorization(cloudEvent, sinkCredential);
    } else {
      cloudEventProducer.sendEvent(cloudEvent);
    }
  }

  private void sendCloudEventHorizon(CloudEvent cloudEvent) {
    try {
      var cloudEventAsJson = writeCloudEventAsJsonString(cloudEvent);
      eventHubClient.sendEvent(cloudEventAsJson);
    } catch (FeignException e) {
      log.error(e.getMessage());
      throw new QodApiException(HttpStatus.SERVICE_UNAVAILABLE, "The eventhub service is currently not available");
    }
  }

  @Generated
  @SneakyThrows
  private CloudEvent buildCloudEvent(SessionInfo session) {
    CloudEventData cloudEventData = new CloudEventData();
    cloudEventData.setSessionId(session.getSessionId());
    cloudEventData.setStatusInfo(session.getStatusInfo());
    cloudEventData.setNotificationUrl(session.getSink());

    if (session.getQosStatus() == QosStatus.AVAILABLE) {
      cloudEventData.setQosStatus(EventQosStatus.AVAILABLE);
    } else {
      cloudEventData.setQosStatus(EventQosStatus.UNAVAILABLE);
    }

    CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1() // v1 specversion
        .withId(UUID.randomUUID().toString())
        .withSubject("")
        .withType(TypeEnum.ORG_CAMARAPROJECT_QUALITY_ON_DEMAND_V0_QOS_STATUS_CHANGED.getValue())
        .withSource(URI.create(cloudEventSourceUrl + "/" + session.getSessionId()))
        .withTime(OffsetDateTime.now(ZoneOffset.UTC).withNano(0))
        .withDataContentType(MediaType.APPLICATION_JSON_VALUE)
        .withData(objectMapper.writeValueAsString(cloudEventData).getBytes(StandardCharsets.UTF_8));
    return cloudEventBuilder.build();
  }


  private void logCloudEvent(CloudEvent cloudEvent) {
    EventFormat format = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
    if (format != null) {
      log.info("Sending Cloud Event: {}", new String(format.serialize(cloudEvent), StandardCharsets.UTF_8));
    }

  }

  @Generated
  @SneakyThrows
  private static String writeCloudEventAsJsonString(CloudEvent cloudEvent) {
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    SimpleModule module = new SimpleModule();
    module.addSerializer(CloudEvent.class, new CloudEventSerializer());
    mapper.registerModule(module);
    return mapper.writeValueAsString(cloudEvent);
  }

}
