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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.camara.quality_on_demand.api.model.AccessTokenCredential;
import com.camara.quality_on_demand.api.model.CloudEvent.TypeEnum;
import com.camara.quality_on_demand.api.model.PlainCredential;
import com.camara.quality_on_demand.api.model.RefreshTokenCredential;
import com.camara.quality_on_demand.api.model.SinkCredential;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
class CloudEventProducerTest {

  private static final String AUTHORIZATION = "Authorization";
  private static final String KAFKA_TOPIC = "kafka_topic";
  private static final String DEFAULT_QOD_TOPIC = "callback";

  @InjectMocks
  private CloudEventProducer cloudEventProducer;

  @Mock
  private KafkaTemplate<String, CloudEvent> kafkaTemplate;

  @Captor
  private ArgumentCaptor<Message<CloudEvent>> captor;

  @BeforeEach
  void setup() throws Exception {
    FieldUtils.writeField(cloudEventProducer, "qodTopic", DEFAULT_QOD_TOPIC, true);
    lenient().when(kafkaTemplate.send(any(Message.class))).thenReturn(completedFuture());
    lenient().when(kafkaTemplate.send(anyString(), any())).thenReturn(completedFuture());
  }

  @Test
  void testSendEvent() {
    cloudEventProducer.sendEvent(createTestCloudEvent());
    verify(kafkaTemplate, times(1)).send(anyString(), any());
  }

  @ParameterizedTest
  @MethodSource("provideCredentials")
  void testSendEventWithAuthorization(SinkCredential credential, String expectedAuthorizationHeader) {
    var cloudEvent = createTestCloudEvent();
    cloudEventProducer.sendEventWithAuthorization(cloudEvent, credential);

    verify(kafkaTemplate, times(1)).send(captor.capture());
    Message<CloudEvent> capturedMessage = captor.getValue();

    assertCapturedMessage(capturedMessage, cloudEvent, expectedAuthorizationHeader);
  }

  @SneakyThrows
  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testSendEventWithUnsupportedRefreshTokenCredentials(CapturedOutput capturedOutput) {
    final var refreshToken = "456ABC";
    final var refreshTokenEndpoint = "example.com/oauth/token";
    var refreshTokenCredential = new RefreshTokenCredential()
        .accessToken("123ABC")
        .accessTokenExpiresUtc(OffsetDateTime.now())
        .refreshToken(refreshToken)
        .refreshTokenEndpoint(new URI(refreshTokenEndpoint));

    var cloudEvent = createTestCloudEvent();
    cloudEventProducer.sendEventWithAuthorization(cloudEvent, refreshTokenCredential);
    verify(kafkaTemplate, times(1)).send(captor.capture());

    assertTrue(capturedOutput.getAll().contains("Unsupported SinkCredential type: "));
  }

  private static Stream<Arguments> provideCredentials() {
    return Stream.of(
        Arguments.of(
            new AccessTokenCredential()
                .accessToken("123ABC")
                .accessTokenExpiresUtc(OffsetDateTime.now()),
            CloudEventProducer.AUTH_BEARER
        ),
        Arguments.of(
            new PlainCredential()
                .secret("123ABC")
                .identifier("t-user"),
            CloudEventProducer.AUTH_BASIC
        )
    );
  }

  private void assertCapturedMessage(
      Message<CloudEvent> capturedMessage,
      CloudEvent expectedEvent,
      String expectedAuthorizationHeader
  ) {
    CloudEvent capturedCloudEvent = capturedMessage.getPayload();
    assertEquals(expectedEvent.getId(), capturedCloudEvent.getId());
    assertEquals(expectedEvent.getSource(), capturedCloudEvent.getSource());
    assertEquals(expectedEvent.getType(), capturedCloudEvent.getType());
    assertEquals(DEFAULT_QOD_TOPIC, capturedMessage.getHeaders().get(KAFKA_TOPIC));
    assertTrue(Objects.requireNonNull(capturedMessage.getHeaders()
            .get(AUTHORIZATION))
        .toString()
        .contains(expectedAuthorizationHeader));
  }

  private <T> CompletableFuture<T> completedFuture() {
    return CompletableFuture.completedFuture(null);
  }

  private static CloudEvent createTestCloudEvent() {
    return CloudEventBuilder.v1()
        .withId(UUID.randomUUID().toString())
        .withSource(URI.create(""))
        .withType(TypeEnum.ORG_CAMARAPROJECT_QUALITY_ON_DEMAND_V0_QOS_STATUS_CHANGED.getValue())
        .build();
  }
}

