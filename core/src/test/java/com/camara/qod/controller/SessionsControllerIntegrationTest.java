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


package com.camara.qod.controller;

import static com.camara.qod.util.SessionsTestData.DURATION_DEFAULT;
import static com.camara.qod.util.SessionsTestData.createNefSubscriptionResponse;
import static com.camara.qod.util.SessionsTestData.createNefSubscriptionResponseWithoutSubscriptionId;
import static com.camara.qod.util.SessionsTestData.createTestSession;
import static com.camara.qod.util.SessionsTestData.createTestSessionWithInvalidAppServerNetwork;
import static com.camara.qod.util.SessionsTestData.createValidTestSession;
import static com.camara.qod.util.SessionsTestData.createValidTestSessionWithGivenDuration;
import static com.camara.qod.util.TestData.objectMapper;
import static java.util.concurrent.TimeUnit.SECONDS;
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

import com.camara.network.api.AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi;
import com.camara.network.api.AsSessionWithQoSApiSubscriptionLevelPostOperationApi;
import com.camara.network.api.model.ProblemDetails;
import com.camara.qod.api.SessionsApi;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.ExtendSessionDuration;
import com.camara.qod.api.model.Message;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.config.NetworkConfig;
import com.camara.qod.config.QodConfig;
import com.camara.qod.exception.QodApiException;
import com.camara.qod.feign.AvailabilityServiceClient;
import com.camara.qod.model.AvailabilityRequest;
import com.camara.qod.model.QosSession;
import com.camara.qod.model.SupportedQosProfiles;
import com.camara.qod.repository.QodSessionRedisRepository;
import com.camara.qod.service.ExpiredSessionMonitor;
import com.camara.qod.service.StorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import feign.FeignException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import redis.embedded.RedisServer;

@SpringBootTest
@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
@ActiveProfiles("test")
class SessionsControllerIntegrationTest {

  private static RedisServer redisServer = null;

  @Value("${network.server.scsasid}")
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
  NetworkConfig networkConfig;
  @Autowired
  QodSessionRedisRepository qosSessionRedisRepository;
  @Autowired
  ExpiredSessionMonitor expiredSessionMonitor;
  @Autowired
  StorageService storage;

  @MockBean
  AvailabilityServiceClient availabilityServiceClient;
  @MockBean
  AsSessionWithQoSApiSubscriptionLevelPostOperationApi postApi;
  @MockBean
  AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi deleteApi;

  static final int MAX_VALUE_SECONDS_PER_DAY = 86399;

  @BeforeAll
  public static void setUp() {
    redisServer = new RedisServer(6370); // RedisServer.builder().port(6370).setting("maxheap 128M").build();
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
    when(availabilityServiceClient.checkSession(any(AvailabilityRequest.class))).thenReturn(ResponseEntity.noContent().build());
    when(availabilityServiceClient.createSession(any(AvailabilityRequest.class))).thenReturn(
        ResponseEntity.status(HttpStatus.CREATED).body("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    when(availabilityServiceClient.deleteSession(any(UUID.class))).thenReturn(ResponseEntity.noContent().build());

    /* Setup NEF-mocks */
    when(postApi.scsAsIdSubscriptionsPost(anyString(), any())).thenReturn(createNefSubscriptionResponse());
  }

  /**
   * Creates, get and delete a session.
   */
  @Test
  void testCreateGetAndDeleteSession_Ok() {
    UUID sessionId = createSession(createValidTestSession());
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

  /**
   * Create and delete sessions by testing all {@link QosProfile}.
   *
   * @param qosProfile the {@link QosProfile}
   */
  @ParameterizedTest
  @EnumSource(SupportedQosProfiles.class)
  void testCreateAndDeleteSession_Ok_ParametrizedQosProfile(SupportedQosProfiles qosProfile) {
    /*Remember old value from QosProfile.L*/
    final int originalFlowIdProfileL = networkConfig.getFlowIdQosL();

    /*Overwrite QosProfile.L by setting the value '6'*/
    ReflectionTestUtils.setField(networkConfig, "flowIdQosL", 6);
    UUID sessionId = createSession(createTestSession(qosProfile));
    deleteSession(sessionId);
    verify(availabilityServiceClient, times(1)).deleteSession(any());

    /*Change back to original value*/
    ReflectionTestUtils.setField(networkConfig, "flowIdQosL", originalFlowIdProfileL);
  }

  /**
   * Create session with unsupported QoS Profile{@link QosProfile}.
   */
  @Test
  void testCreateAndDeleteSession_Unsupported_QosProfile() {
    String unsupportedQosProfile = "QOS_UNSUPPORTED";
    CreateSession createdTestSession = createTestSession(20).qosProfile(unsupportedQosProfile);
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(createdTestSession));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals("Unsupported QosProfile provided: " + unsupportedQosProfile, exception.getMessage());
  }


  /**
   * If AS intersects with any private networks (172..., 192..., 10... ) new session should be created with warning.
   */
  @Test
  void testCreateSession_Created_WithWarning_201() {
    CreateSession session = createValidTestSession();
    session.getApplicationServer().ipv4Address("10.1.0.0/24");
    ResponseEntity<SessionInfo> response = api.createSession(session);
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

  /**
   * Networks need to be defined with the start address (e.g. 200.24.24.0/24 and not 200.24.24.2/24).
   */
  @Test
  void testCreateSession_BadRequest_InvalidIpv4_400() {
    CreateSession session = createTestSessionWithInvalidAppServerNetwork();
    QodApiException exception = assertThrows(QodApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("Network specification for device.ipv4Address.publicAddress not valid 172.24.11.4/18"));
    assertSame(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  /**
   * Create a session with an invalid / blank 'duration'.
   */
  @Test
  void testCreateSession_BadRequest_BlankDuration_400() {
    CreateSession session = createTestSession(SupportedQosProfiles.QOS_L);
    session.setDuration(null);
    QodApiException exception = assertThrows(QodApiException.class, () -> api.createSession(session));
    assertEquals("Validation failed for parameter 'duration'", exception.getMessage());
    assertSame(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  @SneakyThrows
  void testCreateSession_BadRequest_InvalidNotificationUrl_400() {
    CreateSession session = createTestSession(SupportedQosProfiles.QOS_L);
    session.getWebhook().setNotificationUrl(new URI("ads"));
    QodApiException exception = assertThrows(QodApiException.class, () -> api.createSession(session));
    assertEquals("Validation failed for parameter 'notificationUrl' - Invalid URL-Syntax.", exception.getMessage());
    assertSame(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  /**
   * Creates a session with a not permitted Profile QOS_L (flowId = -1), see test-configuration(application-test.yml).
   */
  @Test
  void testCreateSession_BadRequest_InvalidQosProfile_400() {
    CreateSession session = createTestSession(SupportedQosProfiles.QOS_L);
    QodApiException exception = assertThrows(QodApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("QoS profile <QOS_L> unknown or disabled"));
    assertSame(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void testCreateSession_BadRequest_InvalidPortsRange_400() {
    CreateSession session = createTestSession(9000, 8000, 1000);
    QodApiException exception = assertThrows(QodApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("Ports specification not valid"));
    assertSame(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void testCreateSession_Conflict_SessionAlreadyActive_409() {
    final var sessionId = createSession(createValidTestSession());
    assertNotNull(sessionId);
    CreateSession validTestSession = createValidTestSession();
    QodApiException exception = assertThrows(QodApiException.class, () -> api.createSession(validTestSession));
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
  void testCreate_Conflict_OccupiedPorts_409_Created_FreePorts_201() {
    CreateSession session = createTestSession(5000, 5002, 6000);
    UUID sessionId = createSession(session);

    // not permitted because of included address, ports and protocols
    CreateSession validTestSession = createValidTestSession();
    QodApiException exception = assertThrows(QodApiException.class, () -> api.createSession(validTestSession));
    assertTrue(exception.getMessage().contains("already active"));
    assertSame(HttpStatus.CONFLICT, exception.getHttpStatus());

    // permitted because of different ports
    UUID sessionIdTwo = createSession(createTestSession(5003, 5005, 6001));

    deleteSession(sessionId);
    deleteSession(sessionIdTwo);
  }

  @Test
  void testCreateSession_InternalServerError_NoValidSubscription_500() {
    when(postApi.scsAsIdSubscriptionsPost(anyString(), any())).thenReturn(createNefSubscriptionResponseWithoutSubscriptionId());
    CreateSession validTestSession = createValidTestSession();
    QodApiException exception = assertThrows(QodApiException.class, () -> api.createSession(validTestSession));
    assertTrue(exception.getMessage().contains("No valid subscription"));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
  }

  @Test
  void testCreateSession_InternalServerErrorByNef_500_without_response_body() {
    doThrow(new HttpServerErrorException(HttpStatusCode.valueOf(500))).when(postApi).scsAsIdSubscriptionsPost(anyString(), any());
    CreateSession validTestSession = createValidTestSession();
    QodApiException exception = assertThrows(QodApiException.class, () -> api.createSession(validTestSession));
    assertEquals("Error while reading the response body of NEF", exception.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
  }

  @Test
  void testCreateSession_InternalServerErrorByNef_500() {
    ProblemDetails problemDetails = new ProblemDetails();
    problemDetails.setDetail("test error");
    problemDetails.setStatus(500);
    String json;
    try {
      json = objectMapper.writeValueAsString(problemDetails);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Invalid problem details");
    }
    byte[] jsonAsBytes = json.getBytes(StandardCharsets.UTF_8);

    HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatusCode.valueOf(500), "Test Error", jsonAsBytes,
        StandardCharsets.UTF_8);

    doThrow(httpServerErrorException).when(postApi).scsAsIdSubscriptionsPost(anyString(), any());

    doThrow(httpServerErrorException).when(postApi).scsAsIdSubscriptionsPost(anyString(), any());
    CreateSession validTestSession = createValidTestSession();
    QodApiException exception = assertThrows(QodApiException.class, () -> api.createSession(validTestSession));
    assertEquals("NEF/SCEF returned error 500 while creating a subscription on NEF/SCEF: test error", exception.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());

    problemDetails.setDetail(null);
    problemDetails.setCause("test error");

    assertEquals("NEF/SCEF returned error 500 while creating a subscription on NEF/SCEF: test error", exception.getMessage());
  }

  @Test
  void testCreateSession_ServiceUnavailable_Bookkeeper_503() {
    doThrow(FeignException.ServiceUnavailable.class).when(availabilityServiceClient).checkSession(any(AvailabilityRequest.class));
    if (availabilityEnabled) {
      CreateSession validTestSession = createValidTestSession();
      QodApiException exception = assertThrows(QodApiException.class, () -> api.createSession(validTestSession));
      assertEquals("The availability service is currently not available", exception.getMessage());
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    } else {
      UUID sessionId = createSession(createTestSession(SupportedQosProfiles.QOS_E));
      deleteSession(sessionId);
    }
  }

  /* get session */

  @Test
  void testGetSession_NotFound_404() {
    UUID uuid = UUID.randomUUID();
    QodApiException exception = assertThrows(QodApiException.class, () -> api.getSession(uuid));
    assertTrue(exception.getMessage().contains("not found"));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  void testGetSession_NotFound_SessionExpired_404() {
    UUID sessionId = createSession(createValidTestSession());
    Optional<QosSession> sessionOptional = storage.getSession(sessionId);
    assertTrue(sessionOptional.isPresent());
    QosSession session = sessionOptional.get();
    session.setExpiresAt(Instant.now().getEpochSecond());
    storage.saveSession(session);

    expiredSessionMonitor.checkForExpiredSessions();
    await().atMost(DURATION_DEFAULT + 1, SECONDS).untilAsserted(() -> {
      QodApiException exception = assertThrows(QodApiException.class, () -> api.getSession(sessionId));
      assertTrue(exception.getMessage().contains("not found"));
      assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    });
  }

  /* delete session */

  @Test
  void testDeleteSession_NotFound_404() {
    UUID uuid = UUID.randomUUID();
    QodApiException exception = assertThrows(QodApiException.class, () -> api.deleteSession(uuid));
    assertTrue(exception.getMessage().contains("not found"));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  void testDeleteSession_ServiceUnavailable_Nef_503() {
    ProblemDetails problemDetails = new ProblemDetails();
    problemDetails.setDetail("test error");
    problemDetails.setStatus(503);
    String json;
    try {
      json = objectMapper.writeValueAsString(problemDetails);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Invalid problem details");
    }
    byte[] jsonAsBytes = json.getBytes(StandardCharsets.UTF_8);

    HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatusCode.valueOf(503), "test error", jsonAsBytes,
        StandardCharsets.UTF_8);

    doThrow(httpServerErrorException).when(deleteApi).scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(anyString(), any());
    UUID sessionId = createSession(createValidTestSession());
    QodApiException exception = assertThrows(QodApiException.class, () -> api.deleteSession(sessionId));
    assertTrue(exception.getMessage().contains("test error"));
    assertSame(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testDeleteSession_NotFound_Nef_404(CapturedOutput output) {
    ProblemDetails problemDetails = new ProblemDetails();
    problemDetails.setDetail("test error");
    problemDetails.setStatus(404);
    String json;
    try {
      json = objectMapper.writeValueAsString(problemDetails);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Invalid problem details");
    }
    byte[] jsonAsBytes = json.getBytes(StandardCharsets.UTF_8);

    HttpClientErrorException httpClientErrorException = HttpClientErrorException.create(HttpStatusCode.valueOf(404), "test error",
        new HttpHeaders(), jsonAsBytes, StandardCharsets.UTF_8);
    doThrow(httpClientErrorException).when(deleteApi).scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(anyString(), any());
    UUID sessionId = createSession(createValidTestSession());
    assertDoesNotThrow(() -> api.deleteSession(sessionId));
    assertTrue(output.getAll().contains("Problem by calling NEF/SCEF (Possibly already deleted by NEF)"));
  }

  @Test
  void testDeleteSession_UnexpectedException_Nef_500() {
    doThrow(new IllegalArgumentException()).when(deleteApi).scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(anyString(), any());
    UUID sessionId = createSession(createValidTestSession());
    QodApiException exception = assertThrows(QodApiException.class, () -> api.deleteSession(sessionId));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    assertEquals("Unexpected exception occurred during deletion of subscription", exception.getMessage());
  }

  /**
   * Creates, get, extend and delete a session.
   */
  @Test
  void testCreateExtent_CheckAndDeleteSession_Ok() {

    UUID sessionId = createSession(createValidTestSessionWithGivenDuration(20));
    if (availabilityEnabled) {
      verify(availabilityServiceClient, times(1)).checkSession(any());
      verify(availabilityServiceClient, times(1)).createSession(any());
    } else {
      verify(availabilityServiceClient, times(0)).checkSession(any());
      verify(availabilityServiceClient, times(0)).createSession(any());
    }
    assertSame(HttpStatus.OK, api.getSession(sessionId).getStatusCode());
    assertEquals(20, api.getSession(sessionId).getBody().getDuration());
    Long initialStartTime = api.getSession(sessionId).getBody().getStartedAt();

    assertSame(HttpStatus.OK, api.extendQosSessionDuration(sessionId, new ExtendSessionDuration(40)).getStatusCode());
    assertEquals(60, api.getSession(sessionId).getBody().getDuration());
    Long newExpirationTime = api.getSession(sessionId).getBody().getExpiresAt();
    assertEquals(60, newExpirationTime - initialStartTime);
    api.deleteSession(sessionId);
  }

  /**
   * Creates, get, extend to duration limit and delete a session.
   */
  @Test
  void testCreateExtentToLimit_CheckAndDeleteSession_Ok() {
    // duration is limited to maximum: 86399 (seconds  per day)
    UUID sessionId = createSession(createValidTestSessionWithGivenDuration(86300));
    if (availabilityEnabled) {
      verify(availabilityServiceClient, times(1)).checkSession(any());
      verify(availabilityServiceClient, times(1)).createSession(any());
    } else {
      verify(availabilityServiceClient, times(0)).checkSession(any());
      verify(availabilityServiceClient, times(0)).createSession(any());
    }
    assertSame(HttpStatus.OK, api.getSession(sessionId).getStatusCode());
    assertEquals(86300, api.getSession(sessionId).getBody().getDuration());
    Long initialStartTime = api.getSession(sessionId).getBody().getStartedAt();

    assertSame(HttpStatus.OK, api.extendQosSessionDuration(sessionId, new ExtendSessionDuration(600)).getStatusCode());
    assertEquals(MAX_VALUE_SECONDS_PER_DAY, api.getSession(sessionId).getBody().getDuration());
    Long newExpirationTime = api.getSession(sessionId).getBody().getExpiresAt();
    assertEquals(MAX_VALUE_SECONDS_PER_DAY, newExpirationTime - initialStartTime);
    api.deleteSession(sessionId);
  }

  /**
   * Extend a session, expect not found response.
   */
  @Test
  void testExtentSession_NotFound_404() {
    var sessionId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    var extendDuration = new ExtendSessionDuration(40);
    QodApiException exception = assertThrows(QodApiException.class,
        () -> api.extendQosSessionDuration(sessionId, extendDuration));
    assertTrue(exception.getMessage().contains("QoD session not found for session ID: 00000000-0000-0000-0000-000000000000"));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  private UUID createSession(CreateSession createSession) {
    ResponseEntity<SessionInfo> response = api.createSession(createSession);
    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    return response.getBody().getSessionId();
  }

  private void deleteSession(UUID sessionId) {
    ResponseEntity<Void> response = api.deleteSession(sessionId);
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }
}
