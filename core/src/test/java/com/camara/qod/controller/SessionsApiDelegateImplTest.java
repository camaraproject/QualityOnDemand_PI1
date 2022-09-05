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

import com.camara.qod.api.model.*;
import com.camara.qod.config.QodConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.camara.scef.api.model.AsSessionWithQoSSubscription;
import com.camara.scef.api.model.UserPlaneNotificationData;
import com.camara.qod.api.SessionsApiDelegate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@WireMockTest(httpPort = 9000)
class SessionsApiDelegateImplTest {
  private final String asId;
  private final SessionsApiDelegate api;
  private final Boolean maskSensibleData;
  private final Integer defaultDuration;
  private final Boolean bookkeeperEnabled;

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  private final QodConfig qodConfig;
  @Autowired
  public SessionsApiDelegateImplTest(
      @Value("${scef.server.scsasid}") String asId,
      SessionsApiDelegate api,
      @Value("${qod.mask-sensible-data}") Boolean maskSensibleData,
      @Value("${qod.bookkeeper.enabled}") Boolean bookkeeperEnabled,
      QodConfig qodConfig) {
    this.asId = asId;
    this.api = api;
    this.maskSensibleData = maskSensibleData;
    this.defaultDuration = 2;
    this.bookkeeperEnabled = bookkeeperEnabled;
    this.qodConfig = qodConfig;

    MockHttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }
  @Test
  void createAndUpdateDurationOfSession() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForBookkeeperAvailabilityRequest(true);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();
    stubForDeleteSubscription();

    RenewSession renewSession = new RenewSession().duration(20);
    UUID sessionId =
            createSession(
                    session(QosProfile.LOW_LATENCY,
                            "200.24.24.0/24",
                            "5000-5002,6000",
                            qodConfig.getQosExpirationTimeBeforeHandling() + 100));
    updateSession(sessionId, renewSession);
    deleteSession(sessionId);
  }

  @Test
  void updateDurationOfSessionWhichExpires() throws JsonProcessingException, InterruptedException {
    stubForCreateSubscription();
    stubForBookkeeperAvailabilityRequest(true);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();
    stubForDeleteSubscription();

    RenewSession renewSession = new RenewSession().duration(60);
    UUID sessionId =
            createSession(
                    session(QosProfile.LOW_LATENCY,
                            "200.24.24.0/24",
                            "5000-5002,6000",
                            qodConfig.getQosExpirationTimeBeforeHandling()));
    Thread.sleep(qodConfig.getQosExpirationTimeBeforeHandling() * 500L);

    SessionApiException exception =
            assertThrows(SessionApiException.class, () -> api.renewSession(sessionId, renewSession));
    assertTrue(exception.getMessage().contains("will soon expire"));
    deleteSession(sessionId);
  }

  @Test
  void updateUnknownSession() {
    SessionApiException exception =
            assertThrows(SessionApiException.class, () -> api.getSession(UUID.randomUUID()));
    assertTrue(exception.getMessage().contains("not found"));
  }

  @Test
  void getKnownSession() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForBookkeeperAvailabilityRequest(true);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();

    UUID sessionId = createSession(session(QosProfile.LOW_LATENCY));
    assertSame(api.getSession(sessionId).getStatusCode(), HttpStatus.OK);
    api.deleteSession(sessionId);
  }
  @Test
  void getUnknownSession() {
    SessionApiException exception =
        assertThrows(SessionApiException.class, () -> api.getSession(UUID.randomUUID()));
    assertTrue(exception.getMessage().contains("not found"));
    assertSame(exception.getHttpStatus(), HttpStatus.NOT_FOUND);
  }

  @Test
  void createAndDeleteSession() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForBookkeeperAvailabilityRequest(true);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();

    UUID sessionId = createSession(session(QosProfile.LOW_LATENCY));
    deleteSession(sessionId);
  }

  @Test
  void createUnsubscribedSession() throws JsonProcessingException {
    stubForCreateInvalidSubscription();
    stubForDeleteSubscription();
    stubForBookkeeperAvailabilityRequest(true);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();

    SessionApiException exception =
            assertThrows(
                    SessionApiException.class,
                    () -> api.createSession(session(QosProfile.LOW_LATENCY)));
    assertTrue(exception.getMessage().contains("No valid subscription"));
    assertSame(exception.getHttpStatus(), HttpStatus.valueOf(500));
  }

  @Test
  void createSessionBookkeeperCapacityNotAvailable() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForBookkeeperAvailabilityRequest(false);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();

    if (bookkeeperEnabled) {
      SessionApiException exception =
              assertThrows(
                      SessionApiException.class,
                      () -> api.createSession(session(QosProfile.LOW_LATENCY)));
      assertTrue(exception.getMessage().contains("Requested QoS session is currently not available"));
      assertSame(exception.getHttpStatus(), HttpStatus.valueOf(409));
    } else {
      UUID sessionId = createSession(session(QosProfile.LOW_LATENCY));
      deleteSession(sessionId);
    }
  }

  @Test
  void createSessionBookkeeperError() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForBookkeeperAvailabilityErrorRequest();
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();

    SessionApiException exception =
            assertThrows(SessionApiException.class,
                    () -> api.createSession(session(QosProfile.LOW_LATENCY)));
    assertTrue(exception.getMessage().contains("The service is currently not available"));
    assertSame(exception.getHttpStatus(), HttpStatus.valueOf(503));
  }


  @Test
  void createSessionAlreadyActiveNotPermitted() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForBookkeeperAvailabilityRequest(true);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();

    UUID sessionId = createSession(session(QosProfile.LOW_LATENCY));
    SessionApiException exception =
        assertThrows(
            SessionApiException.class, () -> api.createSession(session(QosProfile.LOW_LATENCY)));
    assertTrue(exception.getMessage().contains("already active"));
    assertSame(exception.getHttpStatus(), HttpStatus.valueOf(409));
    if (this.maskSensibleData) {
      assertTrue(exception.getMessage().contains("XXXXXXXX-XXXX-XXXX-XXXX-"));
    }
    deleteSession(sessionId);
  }

  /**
   * New sessions should be validated against rules in order to prevent creating session which QoS
   * profile couldn't be guaranteed.
   */
  @Test
  void createOnlyValidSessions() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForBookkeeperAvailabilityRequest(true);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();

    UUID sessionId =
        createSession(
            session(QosProfile.LOW_LATENCY, "200.24.24.0/24", "5000-5002,6000", defaultDuration));

    // not permitted because of included address, ports and protocols
    SessionApiException exception =
        assertThrows(
            SessionApiException.class,
            () -> api.createSession(session(QosProfile.LOW_LATENCY, "200.24.24.0/26")));
    assertTrue(exception.getMessage().contains("already active"));

    // permitted because of different ports
    UUID sessionIdTwo =
        createSession(session(QosProfile.LOW_LATENCY, "200.24.24.0/24", "6001", defaultDuration));

    deleteSession(sessionId);
    deleteSession(sessionIdTwo);
  }

  @Test
  void createSessionDurationNotValid() {
    CreateSession session1 = session(QosProfile.LOW_LATENCY, "200.24.24.0", "5000", 0);
    CreateSession session2 = session(QosProfile.LOW_LATENCY, "200.24.24.0", "5000", 86401);
    CreateSession session3 = session(QosProfile.LOW_LATENCY, "200.24.24.0", "5000", 1);
    CreateSession session4 = session(QosProfile.LOW_LATENCY, "200.24.24.0", "5000", 86400);

    Set<ConstraintViolation<CreateSession>> violations1 = validator.validate(session1);
    Set<ConstraintViolation<CreateSession>> violations2 = validator.validate(session2);
    Set<ConstraintViolation<CreateSession>> violations3 = validator.validate(session3);
    Set<ConstraintViolation<CreateSession>> violations4 = validator.validate(session4);

    assertEquals(1, violations1.size());
    assertEquals(1, violations2.size());
    assertTrue(violations3.isEmpty());
    assertTrue(violations4.isEmpty());
  }

    /**
     * Networks need to be defined with the start address (e.g. 200.24.24.0/24 and not 200.24.24.2/24)
     */
  @Test
  void createSessionDirtyNetworkDefinitionNotPermitted() {
    SessionApiException exception =
        assertThrows(
            SessionApiException.class,
            () -> api.createSession(session(QosProfile.LOW_LATENCY, "200.24.24.2/24")));
    assertTrue(exception.getMessage().contains("Network specification not valid"));
  }

  @Test
  void createSessionInvalidIPFormat() {
    CreateSession sessionInvalid = session(QosProfile.LOW_LATENCY, ".00.24.24.0");
    CreateSession sessionValid   = session(QosProfile.LOW_LATENCY, "200.24.24.0");

    Set<ConstraintViolation<CreateSession>> violations1 = validator.validate(sessionInvalid);
    Set<ConstraintViolation<CreateSession>> violations2 = validator.validate(sessionValid);

    assertEquals(1, violations1.size());
    assertEquals("asAddr", violations1.iterator().next().getPropertyPath().toString());
    assertTrue(violations2.isEmpty());
  }

  /** Profile THROUGHPUT_L is not permitted, see test configuration (application-test.yml) */
  @Test
  void createSessionQosProfileNotPermitted() {
    SessionApiException exception =
        assertThrows(
            SessionApiException.class, () -> api.createSession(session(QosProfile.THROUGHPUT_L)));
    assertTrue(exception.getMessage().contains("profile unknown"));
  }

  @Test
  void createSessionPortsNotValid() {
    SessionApiException exception =
        assertThrows(SessionApiException.class, () -> api.createSession(
                session(QosProfile.LOW_LATENCY, "200.24.24.3", "1000, 100A", defaultDuration)));
    assertTrue(exception.getMessage().contains("not valid"));
    assertSame(exception.getHttpStatus(), HttpStatus.valueOf(400));
  }

  @Test
  void createSessionPortsRangeNotValid() {
    SessionApiException exception =
        assertThrows(SessionApiException.class, () -> api.createSession(
                session(QosProfile.LOW_LATENCY, "200.24.24.3", "9000-8000, 1000", defaultDuration)));
    assertTrue(exception.getMessage().contains("range not valid"));
    assertSame(exception.getHttpStatus(), HttpStatus.valueOf(400));
  }

  @Test
  void createSessionProtocolNotValid() {
    String wrongValue = "XYZ";
    IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> api.createSession(
                    session(Protocol.fromValue(wrongValue), Protocol.ANY)));
    assertTrue(exception.getMessage().contains("Unexpected value"));
  }

  @Test
  void deleteUnknownSession() {
    SessionApiException exception =
        assertThrows(SessionApiException.class, () -> api.deleteSession(UUID.randomUUID()));
    assertTrue(exception.getMessage().contains("not found"));
    assertSame(exception.getHttpStatus(), HttpStatus.NOT_FOUND);
  }

  @Test
  void deleteSessionBookkeeperError() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForBookkeeperAvailabilityRequest(true);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingErrorRequest();

    UUID sessionId = createSession(
            session(QosProfile.LOW_LATENCY, "200.24.24.3", "1000", 1));

    SessionApiException exception =
            assertThrows(SessionApiException.class,
                    () -> api.deleteSession(sessionId));
    assertTrue(exception.getMessage().contains("The service is currently not available"));
    assertSame(exception.getHttpStatus(), HttpStatus.valueOf(503));

    stubForBookkeeperDeleteBookingRequest();
    api.deleteSession(sessionId);
  }

  @Test
  void deleteSessionNefError() throws JsonProcessingException {
    stubForCreateSubscription();
    stubForDeleteErrorSubscription();
    stubForBookkeeperAvailabilityRequest(true);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();

    UUID sessionId = createSession(
            session(QosProfile.LOW_LATENCY));

    SessionApiException exception =
            assertThrows(SessionApiException.class,
                    () -> api.deleteSession(sessionId));
    assertTrue(exception.getMessage().contains("NEF/SCEF returned error"));
    assertSame(exception.getHttpStatus(), HttpStatus.valueOf(500));
  }

  @Test
  void expireSession() throws JsonProcessingException, InterruptedException {
    stubForCreateSubscription();
    stubForDeleteSubscription();
    stubForBookkeeperAvailabilityRequest(true);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();

    UUID sessionId = createSession(session(QosProfile.LOW_LATENCY));
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
    stubForBookkeeperAvailabilityRequest(true);
    stubForBookkeeperBookingRequest();
    stubForBookkeeperDeleteBookingRequest();
    ResponseEntity<SessionInfo> response = api.createSession(session(QosProfile.LOW_LATENCY, "10.1.0.0/24"));
    if (bookkeeperEnabled) {
      verify(postRequestedFor(urlPathEqualTo("/checkServiceQualification")));
      verify(postRequestedFor(urlPathEqualTo("/service")));
    } else {
      verify(0, postRequestedFor(urlPathEqualTo("/checkServiceQualification")));
      verify(0, postRequestedFor(urlPathEqualTo("/service")));
    }
    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getMessages().get(0));
    assertSame(response.getBody().getMessages().get(0).getSeverity(), Message.SeverityEnum.WARNING);
  }


  private UUID createSession(CreateSession createSession) {
    ResponseEntity<SessionInfo> response = api.createSession(createSession);
    if (bookkeeperEnabled) {
      verify(postRequestedFor(urlPathEqualTo("/checkServiceQualification")));
      verify(postRequestedFor(urlPathEqualTo("/service")));
    } else {
      verify(0, postRequestedFor(urlPathEqualTo("/checkServiceQualification")));
      verify(0, postRequestedFor(urlPathEqualTo("/service")));
    }
    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    return response.getBody().getId();
  }

  private void updateSession(UUID sessionId, RenewSession renewSession) {
    ResponseEntity<SessionInfo> patchedResponse = api.renewSession(sessionId, renewSession);
    assertNotNull(patchedResponse);
    assertEquals(HttpStatus.OK, patchedResponse.getStatusCode());
    ResponseEntity<SessionInfo> response = api.getSession(sessionId);
    sessionFound(sessionId);
    assertEquals(response.getBody().getStartedAt() + renewSession.getDuration(),
            patchedResponse.getBody().getExpiresAt());
  }

  private void sessionFound(UUID sessionId) {
    ResponseEntity<SessionInfo> response = api.getSession(sessionId);
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  private void deleteSession(UUID sessionId) {
    ResponseEntity<Void> response = api.deleteSession(sessionId);
    if (bookkeeperEnabled) {
      verify(deleteRequestedFor(urlPathMatching("/service/([a-zA-Z0-9/-]*)")));
    } else {
      verify(0, deleteRequestedFor(urlPathMatching("/service/([a-zA-Z0-9/-]*)")));
    }
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  private void sessionNotFound(UUID sessionId) {
    SessionApiException exception =
        assertThrows(SessionApiException.class, () -> api.getSession(sessionId));
    assertTrue(exception.getMessage().contains("not found"));
  }

  private void stubForCreateSubscription() throws JsonProcessingException {
    stubFor(
        post("/3gpp-as-session-with-qos/v1/" + asId + "/subscriptions")
            .willReturn(
                created()
                    .withHeader("Content-Type", "application/json")
                    .withBody(subscriptionJsonString())));
  }

  private void stubForCreateInvalidSubscription() throws JsonProcessingException {
    stubFor(
            post("/3gpp-as-session-with-qos/v1/" + asId + "/subscriptions")
                    .willReturn(
                            created()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(subscriptionInvalidJsonString())));
  }

  private void stubForDeleteSubscription() throws JsonProcessingException {
    stubFor(
        delete(
                urlPathMatching(
                    "/3gpp-as-session-with-qos/v1/" + asId + "/subscriptions/([a-zA-Z0-9/-]*)"))
            .willReturn(
                ok().withHeader("Content-Type", "application/json")
                    .withBody(notificationDataJsonString())));
  }

  private void stubForDeleteErrorSubscription() {
    stubFor(
            delete(
                    urlPathMatching(
                            "/3gpp-as-session-with-qos/v1/" + asId + "/subscriptions/([a-zA-Z0-9/-]*)"))
                    .willReturn(
                            serviceUnavailable()));
  }


  private String subscriptionJsonString() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    AsSessionWithQoSSubscription subscription =
        new AsSessionWithQoSSubscription()
            .self("http://foo.com/subscriptions/" + UUID.randomUUID());
    return objectMapper.writeValueAsString(subscription);
  }

  private String subscriptionInvalidJsonString() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    AsSessionWithQoSSubscription subscription =
            new AsSessionWithQoSSubscription()
            .self(null);
    return objectMapper.writeValueAsString(subscription);
  }

  private String notificationDataJsonString() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    UserPlaneNotificationData notificationData = new UserPlaneNotificationData();
    return objectMapper.writeValueAsString(notificationData);
  }

  private void stubForBookkeeperAvailabilityRequest(Boolean isSuccessful) throws JsonProcessingException {
    stubFor(
            post("/checkServiceQualification")
                    .willReturn(
                            created()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(bookkeeperAvailabilityJsonString(isSuccessful))));
  }

  private void stubForBookkeeperAvailabilityErrorRequest() {
    stubFor(
            post("/checkServiceQualification")
                    .willReturn(aResponse()
                            .withFault(Fault.EMPTY_RESPONSE)));
  }

  private void stubForBookkeeperBookingRequest() throws JsonProcessingException {
    stubFor(
            post("/service")
                    .willReturn(
                            created()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(bookkeeperBookingJsonString())));
  }

  private void stubForBookkeeperDeleteBookingRequest() {
    stubFor(
            delete(
                    urlPathMatching(
                            "/service/([a-zA-Z0-9/-]*)"))
                    .willReturn(
                            noContent()));
  }

  private void stubForBookkeeperDeleteBookingErrorRequest() {
    stubFor(
            delete(
                    urlPathMatching(
                            "/service/([a-zA-Z0-9/-]*)"))
                    .willReturn(aResponse()
                            .withFault(Fault.EMPTY_RESPONSE)));
  }

  private String bookkeeperAvailabilityJsonString(Boolean isSuccessful) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    Map<String, String> availabilityData = new HashMap<>();
    availabilityData.put("qualificationResult", isSuccessful ? "qualified" : "unqualified");
    return objectMapper.writeValueAsString(availabilityData);
  }

  private String bookkeeperBookingJsonString() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    Map<String, String> bookingData = new HashMap<>();
    bookingData.put("state", "active");
    bookingData.put("id", "57bccde0-2a81-4b05-ab89-0bb80bb9fe63");
    return objectMapper.writeValueAsString(bookingData);
  }

  private CreateSession session(QosProfile qosProfile) {
    return session(qosProfile, "200.24.24.2");
  }

  private CreateSession session(QosProfile qosProfile, String asAddr) {
    return session(qosProfile, asAddr, null, defaultDuration);
  }

  private CreateSession session(
          QosProfile qosProfile, String asAddr, String uePorts, Integer duration) {
    return session(qosProfile, asAddr, uePorts, duration, Protocol.ANY, Protocol.ANY);
  }

  private CreateSession session(
          Protocol protocolIn, Protocol protocolOut) {
    return session(QosProfile.LOW_LATENCY, "200.24.24.2", null, defaultDuration, protocolIn, protocolOut);
  }

  private CreateSession session(
          QosProfile qosProfile, String asAddr, String uePorts, Integer duration, Protocol protocolIn, Protocol protocolOut) {
    return new CreateSession()
            .ueAddr("172.24.11.4")
            .asAddr(asAddr)
            .duration(duration)
            .uePorts(uePorts)
            .protocolIn(protocolIn)
            .protocolOut(protocolOut)
            .qos(qosProfile)
            .notificationUri(URI.create("http://example.com"))
            .notificationAuthToken("12345");
  }
}