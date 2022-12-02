/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 Contributors | Deutsche Telekom AG to CAMARA a Series of LF Projects, LLC
 * The contributor of this file confirms his sign-off for the
 * Developer Certificate of Origin (http://developercertificate.org).
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

import static com.camara.qod.util.SessionsTestData.AVAILABILITY_SERVICE_URI;
import static com.camara.qod.util.SessionsTestData.createTestSession;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.camara.qod.api.SessionsApi;
import com.camara.qod.api.model.AsId;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.Message;
import com.camara.qod.api.model.PortsSpec;
import com.camara.qod.api.model.PortsSpecRanges;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.config.QodConfig;
import com.camara.qod.config.ScefConfig;
import com.camara.qod.exception.SessionApiException;
import com.camara.scef.api.model.AsSessionWithQoSSubscription;
import com.camara.scef.api.model.UserPlaneNotificationData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import redis.embedded.RedisServer;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
@WireMockTest(httpPort = 9000)
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
  Integer defaultDuration = 2;
  Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
  UUID sessionId;

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

  @Test
  void getKnownSession() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForAvailabilityServiceCheckRequest(true);
    stubForAvailabilityServiceCreateRequest();
    stubForAvailabilityServiceDeleteRequest();

    UUID sessionId = createSession(createTestSession(QosProfile.E));
    assertSame(HttpStatus.OK, api.getSession(sessionId).getStatusCode());
    api.deleteSession(sessionId);
  }

  @Test
  void getUnknownSession() {
    UUID uuid = UUID.randomUUID();
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.getSession(uuid));
    assertTrue(exception.getMessage().contains("not found"));
    assertSame(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @ParameterizedTest
  @EnumSource(QosProfile.class)
  void createAndDeleteSessionWithQosProfiles(QosProfile qosProfile) throws JsonProcessingException {
    ReflectionTestUtils.setField(scefConfig, "flowIdQosL", 6);
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForAvailabilityServiceCheckRequest(true);
    stubForAvailabilityServiceCreateRequest();
    stubForAvailabilityServiceDeleteRequest();

    UUID sessionId = createSession(createTestSession(qosProfile));
    deleteSession(sessionId);
  }

  @Test
  void createUnsubscribedSession() throws JsonProcessingException {
    stubForCreateInvalidSubscription();
    stubForDeleteSubscription();
    stubForAvailabilityServiceCheckRequest(true);
    stubForAvailabilityServiceCreateRequest();
    stubForAvailabilityServiceDeleteRequest();

    CreateSession createSession = createTestSession(QosProfile.E);

    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(createSession));
    assertTrue(exception.getMessage().contains("No valid subscription"));
    assertSame(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
  }

  @Test
  void createSessionBookkeeperCapacityNotAvailable() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForAvailabilityServiceCheckRequest(false);
    stubForAvailabilityServiceCreateRequest();
    stubForAvailabilityServiceDeleteRequest();

    if (availabilityEnabled) {
      CreateSession session = createTestSession(QosProfile.E);
      SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
      assertEquals("The availability service is currently not available", exception.getMessage());
      assertSame(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    } else {
      UUID sessionId = createSession(createTestSession(QosProfile.E));
      deleteSession(sessionId);
    }
  }

  @Test
  void createSessionBookkeeperError() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForBookkeeperAvailabilityErrorRequest();
    stubForAvailabilityServiceCreateRequest();
    stubForAvailabilityServiceDeleteRequest();

    CreateSession session = createTestSession(QosProfile.E);
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
    assertEquals("The availability service is currently not available", exception.getMessage());
    assertSame(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
  }

  @Test
  void createSessionAlreadyActiveNotPermitted() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForAvailabilityServiceCheckRequest(true);
    stubForAvailabilityServiceCreateRequest();
    stubForAvailabilityServiceDeleteRequest();

    sessionId = createSession(createTestSession(QosProfile.E));
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
  void createOnlyValidSessions() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForAvailabilityServiceCheckRequest(true);
    stubForAvailabilityServiceCreateRequest();
    stubForAvailabilityServiceDeleteRequest();

    UUID sessionId = createSession(createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0/24"),
        new PortsSpec().ports(List.of(6000)).ranges(List.of(new PortsSpecRanges().from(5000).to(5002))), defaultDuration));

    // not permitted because of included address, ports and protocols
    CreateSession session = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0/26"));
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("already active"));

    // permitted because of different ports
    UUID sessionIdTwo = createSession(
        createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0/24"), new PortsSpec().ports(Collections.singletonList(6001)),
            defaultDuration));

    deleteSession(sessionId);
    deleteSession(sessionIdTwo);
  }

  @Test
  void createSessionDurationNotValid() {
    CreateSession session1 = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0"),
        new PortsSpec().ports(Collections.singletonList(5000)), 0);
    assertEquals(1, validator.validate(session1).size());

    CreateSession session2 = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0"),
        new PortsSpec().ports(Collections.singletonList(5000)), 86401);
    assertEquals(1, validator.validate(session2).size());

    CreateSession session3 = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0"),
        new PortsSpec().ports(Collections.singletonList(5000)), 1);
    assertTrue(validator.validate(session3).isEmpty());

    CreateSession session4 = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0"),
        new PortsSpec().ports(Collections.singletonList(5000)), 86400);
    assertTrue(validator.validate(session4).isEmpty());

  }

  /**
   * Networks need to be defined with the start address (e.g. 200.24.24.0/24 and not 200.24.24.2/24)
   */
  @Test
  void createSessionDirtyNetworkDefinitionNotPermitted() {
    CreateSession session = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.2/24"));
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("Network specification not valid"));
  }

  @Test
  void createSessionInvalidIpFormat() {
    CreateSession sessionInvalid = createTestSession(QosProfile.E, new AsId().ipv4addr(".00.24.24.0"));
    CreateSession sessionValid = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.0"));

    Set<ConstraintViolation<CreateSession>> violations1 = validator.validate(sessionInvalid);
    Set<ConstraintViolation<CreateSession>> violations2 = validator.validate(sessionValid);

    assertEquals(1, violations1.size());
    assertEquals("asId.ipv4addr", violations1.iterator().next().getPropertyPath().toString());
    assertTrue(violations2.isEmpty());
  }

  /**
   * Profile QOS_L is not permitted, see test configuration (application-test.yml)
   */
  @Test
  void createSessionQosProfileNotPermitted() {
    CreateSession session = createTestSession(QosProfile.L);
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("profile unknown"));
  }

  @Test
  void createSessionPortsRangeNotValid() {
    CreateSession session = createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.3"),
        new PortsSpec().ranges(Collections.singletonList(new PortsSpecRanges().from(9000).to(8000)))
            .ports(Collections.singletonList(1000)), defaultDuration);
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.createSession(session));
    assertTrue(exception.getMessage().contains("not valid"));
    assertSame(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void deleteUnknownSession() {
    UUID uuid = UUID.randomUUID();
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.deleteSession(uuid));
    assertTrue(exception.getMessage().contains("not found"));
    assertSame(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  void deleteSessionBookkeeperError() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForAvailabilityServiceCheckRequest(true);
    stubForAvailabilityServiceCreateRequest();
    stubForAvailabilityServiceDeleteErrorRequest();

    UUID sessionId = createSession(
        createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.3"), new PortsSpec().ports(Collections.singletonList(1000)), 1));

    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.deleteSession(sessionId));
    assertEquals("The availability service is currently not available", exception.getMessage());
    assertSame(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());

    stubForAvailabilityServiceDeleteRequest();
    deleteSession(sessionId);
  }

  @Test
  void deleteSessionNefError() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteErrorSubscription();
    stubForAvailabilityServiceCheckRequest(true);
    stubForAvailabilityServiceCreateRequest();
    stubForAvailabilityServiceDeleteRequest();

    UUID sessionId = createSession(createTestSession(QosProfile.E));

    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.deleteSession(sessionId));
    assertTrue(exception.getMessage().contains("NEF/SCEF returned error"));
    assertSame(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
  }

  @Test
  void expireSession() throws JsonProcessingException, InterruptedException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForAvailabilityServiceCheckRequest(true);
    stubForAvailabilityServiceCreateRequest();
    stubForAvailabilityServiceDeleteRequest();

    UUID sessionId = createSession(createTestSession(QosProfile.E));
    sessionFound(sessionId);
    Thread.sleep(defaultDuration * 1000 + 1);
    sessionNotFound(sessionId);
  }

  /**
   * If AS intersects with any private networks (172..., 192..., 10... ) new session should be created with warning
   */
  @Test
  void createSessionWithWarning() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForAvailabilityServiceCheckRequest(true);
    stubForAvailabilityServiceCreateRequest();
    stubForAvailabilityServiceDeleteRequest();
    ResponseEntity<SessionInfo> response = api.createSession(createTestSession(QosProfile.E, new AsId().ipv4addr("10.1.0.0/24")));
    if (availabilityEnabled) {
      verify(postRequestedFor(urlPathEqualTo(AVAILABILITY_SERVICE_URI + "/check")));
      verify(postRequestedFor(urlPathEqualTo(AVAILABILITY_SERVICE_URI)));
    } else {
      verify(0, postRequestedFor(urlPathEqualTo(AVAILABILITY_SERVICE_URI + "/check")));
      verify(0, postRequestedFor(urlPathEqualTo(AVAILABILITY_SERVICE_URI)));
    }
    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getMessages().get(0));
    assertSame(Message.SeverityEnum.WARNING, response.getBody().getMessages().get(0).getSeverity());
  }

  private UUID createSession(CreateSession createSession) {
    ResponseEntity<SessionInfo> response = api.createSession(createSession);
    if (availabilityEnabled) {
      verify(postRequestedFor(urlPathEqualTo(AVAILABILITY_SERVICE_URI + "/check")));
      verify(postRequestedFor(urlPathEqualTo(AVAILABILITY_SERVICE_URI)));
    } else {
      verify(0, postRequestedFor(urlPathEqualTo(AVAILABILITY_SERVICE_URI + "/check")));
      verify(0, postRequestedFor(urlPathEqualTo(AVAILABILITY_SERVICE_URI)));
    }
    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    return response.getBody().getId();
  }

  private void sessionFound(UUID sessionId) {
    ResponseEntity<SessionInfo> response = api.getSession(sessionId);
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  private void deleteSession(UUID sessionId) {
    ResponseEntity<Void> response = api.deleteSession(sessionId);
    if (availabilityEnabled) {
      verify(deleteRequestedFor(urlPathMatching(AVAILABILITY_SERVICE_URI + "/([a-zA-Z0-9/-]*)")));
    } else {
      verify(0, deleteRequestedFor(urlPathMatching(AVAILABILITY_SERVICE_URI + "/([a-zA-Z0-9/-]*)")));
    }
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  private void sessionNotFound(UUID sessionId) {
    SessionApiException exception = assertThrows(SessionApiException.class, () -> api.getSession(sessionId));
    assertTrue(exception.getMessage().contains("not found"));
  }

  private void stubForCreateSubscription() throws JsonProcessingException {
    stubFor(post("/3gpp-as-session-with-qos/v1/" + asId + "/subscriptions").willReturn(
        created().withHeader("Content-Type", "application/json").withBody(subscriptionJsonString())));
  }

  private void stubForCreateInvalidSubscription() throws JsonProcessingException {
    stubFor(post("/3gpp-as-session-with-qos/v1/" + asId + "/subscriptions").willReturn(
        created().withHeader("Content-Type", "application/json").withBody(subscriptionInvalidJsonString())));
  }

  private void stubForDeleteSubscription() throws JsonProcessingException {
    stubFor(delete(urlPathMatching("/3gpp-as-session-with-qos/v1/" + asId + "/subscriptions/([a-zA-Z0-9/-]*)")).willReturn(
        ok().withHeader("Content-Type", "application/json").withBody(notificationDataJsonString())));
  }

  private void stubForDeleteErrorSubscription() {
    stubFor(delete(urlPathMatching("/3gpp-as-session-with-qos/v1/" + asId + "/subscriptions/([a-zA-Z0-9/-]*)")).willReturn(
        serviceUnavailable()));
  }


  private String subscriptionJsonString() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    AsSessionWithQoSSubscription subscription = new AsSessionWithQoSSubscription().self(
        "https://foo.com/subscriptions/" + UUID.randomUUID());
    return objectMapper.writeValueAsString(subscription);
  }

  private String subscriptionInvalidJsonString() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    AsSessionWithQoSSubscription subscription = new AsSessionWithQoSSubscription().self(null);
    return objectMapper.writeValueAsString(subscription);
  }

  private String notificationDataJsonString() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    UserPlaneNotificationData notificationData = new UserPlaneNotificationData();
    return objectMapper.writeValueAsString(notificationData);
  }

  private void stubForAvailabilityServiceCheckRequest(Boolean isSuccessful) throws JsonProcessingException {
    if (isSuccessful) {
      stubFor(post(AVAILABILITY_SERVICE_URI + "/check").willReturn(
          created().withHeader("Content-Type", "application/json").withStatus(204)));
    } else {
      stubFor(post(AVAILABILITY_SERVICE_URI + "/check").willReturn(
          created().withHeader("Content-Type", "application/json").withStatus(400)));
    }
  }

  private void stubForBookkeeperAvailabilityErrorRequest() {
    stubFor(post("/checkServiceQualification").willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
  }

  private void stubForAvailabilityServiceCreateRequest() throws JsonProcessingException {
    stubFor(post(AVAILABILITY_SERVICE_URI).willReturn(
        created().withHeader("Content-Type", "application/json").withBody("3fa85f64-5717-4562-b3fc-2c963f66afa6")));
  }

  private void stubForAvailabilityServiceDeleteRequest() {
    stubFor(delete(urlPathMatching(AVAILABILITY_SERVICE_URI + "/([a-zA-Z0-9/-]*)")).willReturn(noContent()));
  }

  private void stubForAvailabilityServiceDeleteErrorRequest() {
    stubFor(
        delete(urlPathMatching(AVAILABILITY_SERVICE_URI + "/([a-zA-Z0-9/-]*)")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
  }

}