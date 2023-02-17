/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 - 2023 Contributors | Deutsche Telekom AG to CAMARA a Series of LF
 *             Projects, LLC
 * The contributor of this file confirms his sign-off for the
 * Developer
 *             Certificate of Origin (http://developercertificate.org).
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


package com.camara.qod.controller;

import static com.camara.qod.util.SessionsTestData.DURATION_DEFAULT;
import static com.camara.qod.util.SessionsTestData.createNefSubscriptionResponse;
import static com.camara.qod.util.SessionsTestData.createNefSubscriptionResponseWithoutSubscriptionId;
import static com.camara.qod.util.SessionsTestData.createTestSession;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camara.qod.api.SessionsApi;
import com.camara.qod.api.model.AsId;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.Message;
import com.camara.qod.api.model.PortsSpec;
import com.camara.qod.api.model.PortsSpecRangesInner;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.config.QodConfig;
import com.camara.qod.config.ScefConfig;
import com.camara.qod.exception.SessionApiException;
import com.camara.qod.feign.AvailabilityServiceClient;
import com.camara.qod.model.AvailabilityRequest;
import com.camara.qod.repository.QodSessionRedisRepository;
import com.camara.scef.api.AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi;
import com.camara.scef.api.AsSessionWithQoSApiSubscriptionLevelPostOperationApi;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import redis.embedded.RedisServer;

@SpringBootTest
@ActiveProfiles("test")
class SessionsControllerIntegrationTest {

  private static RedisServer redisServer = null;

  @Value("${scef.server.scsasid}")
  String asId;
  @Autowired
  SessionsApi api;
  @Value("${qod.mask-sensible-data}")
  Boolean maskSensibleData;
  @Value("${qod.availability.enabled}")
  Boolean availabilityEnabled;
  @Autowired
  QodConfig qodConfig;
  @Autowired
  ScefConfig scefConfig;
  @Autowired
  QodSessionRedisRepository qosSessionRedisRepository;

  @MockBean
  AvailabilityServiceClient availabilityServiceClient;
  @MockBean
  AsSessionWithQoSApiSubscriptionLevelPostOperationApi postApi;
  @MockBean
  AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi deleteApi;

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

  @BeforeEach
  public void setUpTest() {
    /*Clean up database*/
    qosSessionRedisRepository.deleteAll();

    /* Setup Availability-mocks */
    when(availabilityServiceClient.checkSession(any(AvailabilityRequest.class)))
        .thenReturn(ResponseEntity.noContent().build());
    when(availabilityServiceClient.createSession(any(AvailabilityRequest.class)))
        .thenReturn(ResponseEntity
            .status(HttpStatus.CREATED)
            .body("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    when(availabilityServiceClient.deleteSession(any(UUID.class)))
        .thenReturn(ResponseEntity.noContent().build());

    /* Setup NEF-mocks */
    when(postApi.scsAsIdSubscriptionsPost(anyString(), any())).thenReturn(createNefSubscriptionResponse());
  }

  @Test
  void testCreateGetAndDeleteSession() {
    UUID sessionId = createSession(createTestSession(QosProfile.E));
    if (availabilityEnabled) {
      verify(availabilityServiceClient, times(1)).checkSession(any());
      verify(availabilityServiceClient, times(1)).createSession(any());
    } else {
      verify(availabilityServiceClient, times(0)).checkSession(any());
      verify(availabilityServiceClient, times(0)).createSession(any());
    }
    assertSame(HttpStatus.OK, api.getSession(sessionId).getStatusCode());
    api.deleteSession(sessionId);
  }

  @Test
  void testGetSession_NotFound_404() {
    UUID uuid = UUID.randomUUID();
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.getSession(uuid));
    assertTrue(exception.getMessage().contains("not found"));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @ParameterizedTest
  @EnumSource(QosProfile.class)
  void testCreateAndDeleteSessionWithQosProfiles(QosProfile qosProfile) {
    ReflectionTestUtils.setField(scefConfig, "flowIdQosL", 6);
    UUID sessionId = createSession(createTestSession(qosProfile));
    deleteSession(sessionId);
    verify(availabilityServiceClient, times(1)).deleteSession(any());
  }

  @Test
  void testCreateSession_UnsubscribedSession_500() {
    when(postApi.scsAsIdSubscriptionsPost(anyString(), any())).thenReturn(createNefSubscriptionResponseWithoutSubscriptionId());

    CreateSession createSession = createTestSession(QosProfile.E);

    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(createSession));
    assertTrue(exception.getMessage().contains("No valid subscription"));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
  }

  @Test
  void testCreateSession_BookkeeperCapacityNotAvailable_503() {
    doThrow(FeignException.ServiceUnavailable.class)
        .when(availabilityServiceClient)
        .checkSession(any(AvailabilityRequest.class));
    if (availabilityEnabled) {
      CreateSession session = createTestSession(QosProfile.E);
      SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
      assertEquals("The availability service is currently not available", exception.getMessage());
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    } else {
      UUID sessionId = createSession(createTestSession(QosProfile.E));
      deleteSession(sessionId);
    }
  }

  @Test
  void testCreateSession_AlreadyActiveNotPermitted_409() {
    final var sessionId = createSession(createTestSession(QosProfile.E));
    assertNotNull(sessionId);
    CreateSession session = createTestSession(QosProfile.E);
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("already active"));
    assertSame(HttpStatus.CONFLICT, exception.getHttpStatus());
    if (this.maskSensibleData) {
      assertTrue(exception.getMessage().contains("XXXXXXXX-XXXX-XXXX-XXXX-"));
    }
    deleteSession(sessionId);
  }


  /**
   * New sessions should be validated against rules in order to prevent creating session which QoS profile couldn't be guaranteed.
   */
  @Test
  void testCreateOnlyValidSessions() {
    UUID sessionId = createSession(createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0/24"),
        new PortsSpec().ports(List.of(6000)).ranges(List.of(new PortsSpecRangesInner().from(5000).to(5002))), DURATION_DEFAULT));

    // not permitted because of included address, ports and protocols
    CreateSession session = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0/26"));
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("already active"));

    // permitted because of different ports
    UUID sessionIdTwo = createSession(
        createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0/24"), new PortsSpec().ports(Collections.singletonList(6001)),
            DURATION_DEFAULT));

    deleteSession(sessionId);
    deleteSession(sessionIdTwo);
  }

  @Test
  void testCreateSessionDurationNotValid() {
    CreateSession session = createTestSession(QosProfile.E);

    session.setDuration(0);
    ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> createSession(session));
    assertEquals(1, exception.getConstraintViolations().size());

    session.setDuration(86401);
    exception = assertThrows(ConstraintViolationException.class, () -> createSession(session));
    assertEquals(1, exception.getConstraintViolations().size());

    session.setDuration(1);
    UUID uuid = assertDoesNotThrow(() -> createSession(session));
    deleteSession(uuid);

    session.setDuration(86400);
    assertDoesNotThrow(() -> createSession(session));
  }


  /**
   * Networks need to be defined with the start address (e.g. 200.24.24.0/24 and not 200.24.24.2/24)
   */
  @Test
  void testCreateSessionDirtyNetworkDefinitionNotPermitted() {
    CreateSession session = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.2/24"));
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("Network specification not valid"));
  }

  @Test
  void testCreateSessionInvalidIpFormat() {
    CreateSession sessionInvalid = createTestSession(QosProfile.E, new AsId().ipv4addr(".00.24.24.0"));
    CreateSession sessionValid = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0"));

    ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> createSession(sessionInvalid));
    assertEquals(1, exception.getConstraintViolations().size());
    assertDoesNotThrow(() -> createSession(sessionValid));
  }


  /**
   * Profile QOS_L is not permitted, see test-configuration(application-test.yml)
   */
  @Test
  void testCreateSessionQosProfileNotPermitted() {
    CreateSession session = createTestSession(QosProfile.L);
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("profile unknown"));
  }

  @Test
  void testCreateSessionPortsRangeNotValid() {
    CreateSession session = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.3"),
        new PortsSpec().ranges(Collections.singletonList(new PortsSpecRangesInner().from(9000).to(8000)))
            .ports(Collections.singletonList(1000)), DURATION_DEFAULT);
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("not valid"));
    assertSame(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void testDeleteSession_UnknownSession_404() {
    UUID uuid = UUID.randomUUID();
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.deleteSession(uuid));
    assertTrue(exception.getMessage().contains("not found"));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  void testDeleteSession_NefUnavailable_503() {
    doThrow(HttpServerErrorException.ServiceUnavailable.class)
        .when(deleteApi)
        .scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(anyString(), any());

    UUID sessionId = createSession(createTestSession(QosProfile.E));
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.deleteSession(sessionId));
    assertTrue(exception.getMessage().contains("NEF/SCEF returned error"));
    assertSame(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
  }

  @Test
  void testExpireSession() {
    UUID sessionId = createSession(createTestSession(QosProfile.E));
    getSession(sessionId);
    await()
        .atMost(Duration.of(DURATION_DEFAULT * 1000 + 1, ChronoUnit.MILLIS))
        .untilAsserted(() -> {
          SessionApiException exception = assertThrows(SessionApiException.class, () -> api.getSession(sessionId));
          assertTrue(exception.getMessage().contains("not found"));
        });
  }

  /**
   * If AS intersects with any private networks (172..., 192..., 10... ) new session should be created with warning
   */
  @Test
  void testCreateSessionWithWarning() {
    ResponseEntity<SessionInfo> response = api.createSession(createTestSession(QosProfile.E, new AsId().ipv4addr("10.1.0.0/24")));
    if (availabilityEnabled) {
      verify(availabilityServiceClient, times(1)).checkSession(any());
      verify(availabilityServiceClient, times(1)).createSession(any());
    } else {
      verify(availabilityServiceClient, times(0)).checkSession(any());
      verify(availabilityServiceClient, times(0)).createSession(any());
    }
    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getMessages().get(0));
    assertSame(Message.SeverityEnum.WARNING, response.getBody().getMessages().get(0).getSeverity());
  }

  private UUID createSession(CreateSession createSession) {
    ResponseEntity<SessionInfo> response = api.createSession(createSession);
    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    return response.getBody().getId();
  }

  private void getSession(UUID sessionId) {
    ResponseEntity<SessionInfo> response = api.getSession(sessionId);
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  private void deleteSession(UUID sessionId) {
    ResponseEntity<Void> response = api.deleteSession(sessionId);
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

}
