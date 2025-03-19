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

package com.camara.kafka;

import com.camara.quality_on_demand.api.model.AccessTokenCredential;
import com.camara.quality_on_demand.api.model.PlainCredential;
import com.camara.quality_on_demand.api.model.SinkCredential;
import io.cloudevents.CloudEvent;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudEventProducer {

  public static final String AUTH_BEARER = "Bearer";
  public static final String AUTH_BASIC = "Basic";
  public static final String AUTH_HEADER_FORMAT = "%s %s";
  private final KafkaTemplate<String, CloudEvent> kafkaTemplate;

  @Value("${kafka.topic.webhook}")
  private String qodTopic;

  @Generated
  public void sendEvent(CloudEvent cloudEvent) {
    log.info("Sending Qod CloudEvent: {} by topic: {}", cloudEvent, qodTopic);
    kafkaTemplate.send(qodTopic, cloudEvent);
  }

  /**
   * Sends a {@link CloudEvent} including an {@link SinkCredential}.
   *
   * @param cloudEvent     the cloudEvent
   * @param sinkCredential the credentials
   */
  public void sendEventWithAuthorization(CloudEvent cloudEvent, SinkCredential sinkCredential) {
    log.info("Sending QoD CloudEvent with authorization: {} by topic: {}", cloudEvent, qodTopic);

    var messageBuilder = MessageBuilder.withPayload(cloudEvent)
        .setHeader(KafkaHeaders.TOPIC, qodTopic);

    switch (sinkCredential) {
      case AccessTokenCredential accessTokenCredential -> messageBuilder.setHeader(HttpHeaders.AUTHORIZATION,
          createAuthHeader(AUTH_BEARER, accessTokenCredential.getAccessToken()));
      case PlainCredential plainCredential -> {
        String credentials = plainCredential.getIdentifier() + ":" + plainCredential.getSecret();
        String base64EncodedCredentials = encodeToBase64(credentials);
        messageBuilder.setHeader(HttpHeaders.AUTHORIZATION, createAuthHeader(AUTH_BASIC, base64EncodedCredentials));
      }
      default -> log.warn("Unsupported SinkCredential type: {}", sinkCredential.getClass().getSimpleName());
    }

    kafkaTemplate.send(messageBuilder.build());
  }

  private String createAuthHeader(String authType, String credentials) {
    return String.format(AUTH_HEADER_FORMAT, authType, credentials);
  }

  private String encodeToBase64(String credentials) {
    return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }
}
