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

import static com.camara.util.QosProfilesTestData.PROFILE_MAX_DURATION;
import static com.camara.util.QosProfilesTestData.getQosProfileEntity;
import static com.camara.util.SessionsTestData.DURATION_DEFAULT;
import static com.camara.util.SessionsTestData.TEST_DEVICE_IPV4_ADDRESS;
import static com.camara.util.SessionsTestData.createDefaultTestSessionWithUnknownIpv4;
import static com.camara.util.SessionsTestData.createNefSubscriptionResponse;
import static com.camara.util.SessionsTestData.createNefSubscriptionResponseWithoutSubscriptionId;
import static com.camara.util.SessionsTestData.createQosSessionTestData;
import static com.camara.util.SessionsTestData.createTestSession;
import static com.camara.util.SessionsTestData.createValidTestSession;
import static com.camara.util.TestData.createHttpClientErrorException;
import static com.camara.util.TestData.objectMapper;
import static com.mongodb.assertions.Assertions.assertFalse;
import static com.mongodb.assertions.Assertions.assertNull;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camara.config.NetworkConfig;
import com.camara.config.QodConfig;
import com.camara.entity.QosProfile;
import com.camara.entity.QosSession;
import com.camara.exception.ErrorCode;
import com.camara.exception.QodApiException;
import com.camara.model.SupportedQosProfiles;
import com.camara.network.api.AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi;
import com.camara.network.api.AsSessionWithQoSApiSubscriptionLevelPostOperationApi;
import com.camara.network.api.model.ProblemDetails;
import com.camara.qos_profiles.api.model.Duration;
import com.camara.qos_profiles.api.model.QosProfileStatusEnum;
import com.camara.qos_profiles.api.model.TimeUnitEnum;
import com.camara.quality_on_demand.api.model.CreateSession;
import com.camara.quality_on_demand.api.model.Device;
import com.camara.quality_on_demand.api.model.DeviceIpv4Addr;
import com.camara.quality_on_demand.api.model.PortsSpec;
import com.camara.quality_on_demand.api.model.PortsSpecRangesInner;
import com.camara.quality_on_demand.api.model.QosStatus;
import com.camara.quality_on_demand.api.model.SessionInfo;
import com.camara.quality_on_demand.api.model.StatusInfo;
import com.camara.repository.QosProfileRepository;
import com.camara.repository.QosSessionRepository;
import com.camara.util.SessionsTestData;
import com.camara.util.TokenTestData;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;

@SpringBootTest
@ActiveProfiles("test")
class SessionServiceTest {

  private static final int SECONDS_OF_A_DAY = 86399;
  private QosSession qosSessionTestData;
  @Autowired
  private QodConfig qodConfig;

  @Autowired
  private SessionService sessionService;

  @MockitoBean
  private QosSessionRepository qosSessionRepository;

  @MockitoBean
  private QosProfileRepository qosProfileRepository;

  @MockitoBean
  private TokenService tokenService;

  @Autowired
  private NetworkConfig networkConfig;

  @MockitoBean
  private EventHubService eventHubService;

  @MockitoBean
  private AsSessionWithQoSApiSubscriptionLevelPostOperationApi postApi;

  @MockitoBean
  private AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi deleteApi;

  @MockitoBean
  private ExpiredSessionMonitor expiredSessionMonitor;

  private String savedSessionId;
  private String savedSubscriptionId;

  @SneakyThrows
  @BeforeEach
  public void setUpTest() {
    /* Setup NEF-mocks */
    when(postApi.scsAsIdSubscriptionsPost(anyString(), any())).thenReturn(createNefSubscriptionResponse());

    when(eventHubService.sendEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
    qosSessionTestData = SessionsTestData.createQosSessionTestData();
    when(qosSessionRepository.findBySessionId(any())).thenReturn(Optional.of(qosSessionTestData));
    when(qosSessionRepository.findBySubscriptionId(any())).thenReturn(Optional.of(qosSessionTestData));
    when(qosProfileRepository.findByName(any())).thenReturn(Optional.of(getQosProfileEntity(qosSessionTestData.getQosProfile())));
    when(qosSessionRepository.save(any())).thenReturn(qosSessionTestData);
    when(tokenService.retrieveClientId()).thenReturn(TokenTestData.TEST_CLIENT_ID);
    savedSessionId = qosSessionTestData.getSessionId();
    savedSubscriptionId = qosSessionTestData.getSubscriptionId();
  }

  @Test
  void testCreateSession_Ok() {
    var sessionInfo = assertDoesNotThrow(() -> createSession(createValidTestSession()));
    assertEquals(QosStatus.REQUESTED, sessionInfo.getQosStatus());
    assertNull(sessionInfo.getStartedAt());
    assertNotNull(sessionInfo.getExpiresAt());
    assertNotNull(sessionInfo.getSessionId());
  }

  @Test
  void testCreateSession_QosStatus_Available_And_SendEvent() {
    networkConfig.setSupportedEventResourceAllocation(false);

    SessionInfo sessionInfo = assertDoesNotThrow(() -> createSession(createValidTestSession()));
    assertEquals(QosStatus.AVAILABLE, sessionInfo.getQosStatus());
    verify(eventHubService, times(1)).sendEvent(any());
    qosSessionRepository.deleteBySessionId(sessionInfo.getSessionId().toString());

    networkConfig.setSupportedEventResourceAllocation(true);
  }

  @Test
  void testCreateSession_DeviceNotInResponse_Ok() {
    var sessionInfo = assertDoesNotThrow(() -> sessionService.createSession(createValidTestSession(), false));
    assertNull(sessionInfo.getDevice());
  }


  /**
   * Creates session by testing all {@link SupportedQosProfiles}.
   *
   * @param qosProfile the {@link SupportedQosProfiles}
   */
  @ParameterizedTest
  @EnumSource(SupportedQosProfiles.class)
  void testCreateSession_Ok_ParametrizedQosProfile(SupportedQosProfiles qosProfile) {
    CreateSession validTestSession = createValidTestSession();
    validTestSession.qosProfile(qosProfile.name());
    assertDoesNotThrow(() -> createSession(validTestSession));
  }

  /**
   * Create a session with unsupported QoS Profile.
   */
  @Test
  void testCreateSession_Unsupported_QosProfile() {
    String unsupportedQosProfile = "QOS_UNSUPPORTED";
    CreateSession sessionWithInvalidProfile = createValidTestSession().qosProfile(unsupportedQosProfile);
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(sessionWithInvalidProfile));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals("Unsupported QosProfile provided: " + unsupportedQosProfile, exception.getMessage());
  }


  /**
   * Creates a session with a not permitted Profile QOS_L (flowId = -1), see test-configuration (application-test.yml).
   */
  @Test
  void testCreateSession_BadRequest_InvalidQosProfile_400() {
    final int originalFlowIdProfileL = networkConfig.getFlowIdQosL();

    networkConfig.setFlowIdQosL(-1);

    CreateSession session = createTestSession(SupportedQosProfiles.QOS_L);
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(session));
    assertTrue(exception.getMessage().contains("QoS profile <QOS_L> unknown or disabled"));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());

    networkConfig.setFlowIdQosL(originalFlowIdProfileL);
  }


  /**
   * Creates a session with an inactive QoS profile.
   */
  @Test
  void testCreateSession_BadRequest_InactiveQosProfile_400() {
    SupportedQosProfiles profile = SupportedQosProfiles.QOS_E;
    QosProfile inactiveProfile = getQosProfileEntity(profile.name());
    inactiveProfile.setStatus(QosProfileStatusEnum.INACTIVE);

    when(qosProfileRepository.findByName(profile.name())).thenReturn(Optional.of(inactiveProfile));

    CreateSession session = createTestSession(profile);
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(session));
    assertEquals("Requested QoS profile currently unavailable", exception.getMessage());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  /**
   * Creates a session with a duration greater than the max one for QoS profile.
   */
  @Test
  void testCreateSession_BadRequest_DurationOutOfRangeForQosProfile_Max_400() {
    SupportedQosProfiles profile = SupportedQosProfiles.QOS_L;
    var qosProfile = getQosProfileEntity(profile.name());

    CreateSession session = createTestSession(profile);
    session.duration(qosProfile.getMaxDuration().getValue() + 1);
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(session));
    assertEquals("The requested duration is out of the allowed range for the specific QoS profile: QOS_L", exception.getMessage());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  /**
   * Creates a session with a duration shorter than the min one for QoS profile.
   */
  @Test
  void testCreateSession_BadRequest_DurationOutOfRangeForQosProfile_Min_400() {
    SupportedQosProfiles profile = SupportedQosProfiles.QOS_L;
    var qosProfile = getQosProfileEntity(profile.name());
    CreateSession session = createTestSession(profile);
    session.duration(qosProfile.getMinDuration().getValue() - 1);
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(session));
    assertEquals("The requested duration is out of the allowed range for the specific QoS profile: QOS_L", exception.getMessage());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }


  /**
   * Creates a session with a QoS profile duration in a different time units.
   */
  @ParameterizedTest
  @EnumSource(TimeUnitEnum.class)
  void testCreateSession_DifferentDurationTimeUnits_200(TimeUnitEnum unit) {
    qosProfileRepository.deleteAll();
    SupportedQosProfiles profile = SupportedQosProfiles.QOS_L;
    var qosProfile = getQosProfileEntity(profile.name());
    qosProfile.setMaxDuration(new Duration().unit(unit).value((int) (DURATION_DEFAULT * (Math.pow(10, 9)))));

    when(qosProfileRepository.findByName(any())).thenReturn(Optional.of(qosProfile));
    CreateSession session = createTestSession(profile);
    if (unit != TimeUnitEnum.NANOSECONDS) {
      assertDoesNotThrow(() -> createSession(session));
    } else {
      QodApiException exception = assertThrows(QodApiException.class, () -> createSession(session));
      assertEquals("The requested duration is out of the allowed range for the specific QoS profile: QOS_L", exception.getMessage());
      assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }
  }

  @Test
  void testCreateSession_BadRequest_InvalidPortsRange_400() {
    CreateSession session = createTestSession(9000, 8000, 1000);
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(session));
    assertTrue(exception.getMessage().contains("Ports specification not valid"));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void testCreateSession_() {
    var sessionInfo = assertDoesNotThrow(() -> createSession(createValidTestSession()));
    assertEquals(QosStatus.REQUESTED, sessionInfo.getQosStatus());
    assertNull(sessionInfo.getStartedAt());
    assertNotNull(sessionInfo.getExpiresAt());
    assertNotNull(sessionInfo.getSessionId());
  }

  @Test
  void testCreateSession_Conflict_SessionAlreadyActive_409() {
    CreateSession validTestSession = createValidTestSession();

    qosSessionTestData.setApplicationServer(validTestSession.getApplicationServer());
    when(qosSessionRepository.findByDeviceIpv4addr(any())).thenReturn(List.of(qosSessionTestData));
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(validTestSession));
    assertTrue(exception.getMessage().contains("already active"));
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    if (qodConfig.isQosMaskSensibleData()) {
      assertTrue(exception.getMessage().contains("XXXXXXXX-XXXX-XXXX-XXXX-"));
    }
  }

  @Test
  void testCreateSession_Conflict_OccupiedPorts_409_Created_FreePorts_201() {
    CreateSession validTestSession = createValidTestSession();

    var existingSession = createQosSessionTestData();
    existingSession.setApplicationServer(validTestSession.getApplicationServer());

    when(qosSessionRepository.findByDeviceIpv4addr(any())).thenReturn(List.of(existingSession));
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(validTestSession));
    assertTrue(exception.getMessage().contains("already active"));
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());

    existingSession.setDevicePorts(new PortsSpec()
        .ports(List.of(6001))
        .ranges(List.of(new PortsSpecRangesInner().from(5003).to(5005)))
    );
    when(qosSessionRepository.findByDeviceIpv4addr(any())).thenReturn(List.of(existingSession));

    validTestSession.setDevicePorts(new PortsSpec()
        .ports(List.of(4001))
        .ranges(List.of(new PortsSpecRangesInner().from(4003).to(4005)))
    );
    assertDoesNotThrow(() -> createSession(validTestSession));
  }

  @Test
  void testCreateSession_Device_PortSpecsDefinedWithPortsAndRanges() {
    CreateSession session = createTestSession(40);
    session.setDevicePorts(new PortsSpec());
    assertDoesNotThrow(() -> createSession(session));
    session.setDevicePorts(new PortsSpec().ports(List.of(-1, 70000)));
    assertDoesNotThrow(() -> createSession(session));
  }

  @Test
  void testCreateSession_Device_PortsNotValid_400() {
    CreateSession session = createTestSession(40);
    session.setDevicePorts(new PortsSpec()
        .ports(List.of(-1, 70000))
        .ranges(List.of(new PortsSpecRangesInner()
            .from(5000)
            .to(5022))
        )
    );
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(session));
    assertEquals("Ports ranges are not valid (0-65535)", exception.getMessage());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals(ErrorCode.VALIDATION_FAILED.name(), exception.getErrorCode());
  }

  @Test
  void testCreateSession_InternalServerError_NoValidSubscription_500() {
    when(postApi.scsAsIdSubscriptionsPost(anyString(), any())).thenReturn(createNefSubscriptionResponseWithoutSubscriptionId());
    CreateSession validTestSession = createValidTestSession();
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(validTestSession));
    assertTrue(exception.getMessage().contains("No valid subscription"));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
  }


  @Test
  void testCreateSession_InternalServerErrorByNef_500_without_response_body() {
    doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(500))).when(postApi).scsAsIdSubscriptionsPost(anyString(), any());
    CreateSession validTestSession = createValidTestSession();
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(validTestSession));
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

    HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatusCode.valueOf(500), "Test Error",
        jsonAsBytes,
        StandardCharsets.UTF_8);

    doThrow(httpClientErrorException).when(postApi).scsAsIdSubscriptionsPost(anyString(), any());

    doThrow(httpClientErrorException).when(postApi).scsAsIdSubscriptionsPost(anyString(), any());
    CreateSession validTestSession = createValidTestSession();
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(validTestSession));
    assertEquals("NEF/SCEF returned error 500 while calling NEF/SCEF: test error", exception.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());

    problemDetails.setDetail(null);
    problemDetails.setCause("test error");

    assertEquals("NEF/SCEF returned error 500 while calling NEF/SCEF: test error", exception.getMessage());
  }

  @Test
  void testCreateSession_InternalServerErrorByNef_500_PermanentFailures() {
    ProblemDetails problemDetails = new ProblemDetails();
    problemDetails.setDetail("Permanent Failures");
    problemDetails.setStatus(500);
    String json;
    try {
      json = objectMapper.writeValueAsString(problemDetails);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Invalid problem details");
    }
    byte[] jsonAsBytes = json.getBytes(StandardCharsets.UTF_8);

    HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatusCode.valueOf(500), "Permanent Failures",
        jsonAsBytes, StandardCharsets.UTF_8);

    doThrow(httpClientErrorException).when(postApi).scsAsIdSubscriptionsPost(anyString(), any());
    CreateSession validTestSession = createDefaultTestSessionWithUnknownIpv4();
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(validTestSession));
    assertEquals("NEF/SCEF returned error 500 while calling NEF/SCEF: Probably unknown IPv4 address for UE",
        exception.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
  }

  @Test
  void testCreateSession_InternalServerErrorByNef_500_NoDetailsInResponse() {
    ProblemDetails problemDetails = new ProblemDetails();
    problemDetails.setCause("Just a cause");
    problemDetails.setStatus(500);
    String json;
    try {
      json = objectMapper.writeValueAsString(problemDetails);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Invalid problem details");
    }
    byte[] jsonAsBytes = json.getBytes(StandardCharsets.UTF_8);

    HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatusCode.valueOf(500), "Permanent Failures",
        jsonAsBytes, StandardCharsets.UTF_8);

    doThrow(httpClientErrorException).when(postApi).scsAsIdSubscriptionsPost(anyString(), any());
    CreateSession validTestSession = createDefaultTestSessionWithUnknownIpv4();
    QodApiException exception = assertThrows(QodApiException.class, () -> createSession(validTestSession));
    assertEquals("NEF/SCEF returned error 500 while calling NEF/SCEF: Just a cause",
        exception.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
  }

  @Test
  void testGetSessionInfoById_Ok() {
    when(qosSessionRepository.findAllByClientId(any())).thenReturn(List.of(qosSessionTestData));
    var retrievedSession = assertDoesNotThrow(() -> sessionService.getSessionInfoById(UUID.fromString(savedSessionId)));
    assertEquals(savedSessionId, retrievedSession.getSessionId().toString());
  }

  @Test
  void testGetSessionInfoById_NotFound_404() {
    UUID uuid = UUID.randomUUID();
    QodApiException exception = assertThrows(QodApiException.class, () -> sessionService.getSessionInfoById(uuid));
    assertTrue(exception.getMessage().contains("The specified session does not exist"));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  void testGetSessionInfoListByDevice_WithoutClientId_Ok() {
    when(qosSessionRepository.findAllByClientId(any())).thenReturn(Collections.emptyList());
    var sessionInfoList = assertDoesNotThrow(() -> sessionService.getSessionsByDevice(new Device()
        .ipv4Address(
            new DeviceIpv4Addr().publicAddress(TEST_DEVICE_IPV4_ADDRESS)
        )));
    assertTrue(sessionInfoList.isEmpty());
  }

  @Test
  void testGetSessionInfoListByDevice_WithClientId_Ok() {
    /*Same Device*/
    final var session_1 = createQosSessionTestData();
    session_1.getApplicationServer().setIpv4Address("10.10.10.1");

    final var session_2 = createQosSessionTestData();
    session_2.getApplicationServer().setIpv4Address("10.10.10.2");

    /*Different Device*/
    final var differentDevice = createQosSessionTestData();
    differentDevice.getDevice().getIpv4Address().setPublicAddress("127.0.0.0");
    differentDevice.getApplicationServer().setIpv4Address("120.0.0");

    when(qosSessionRepository.findByDeviceIpv4addr(any())).thenReturn(List.of(session_1, session_2));

    /*Test getSessionsByDevice*/
    Device device = session_1.getDevice();
    var sessionInfoList = assertDoesNotThrow(() -> sessionService.getSessionsByDevice(device));
    List<String> sessionIds = sessionInfoList.stream().map(SessionInfo::getSessionId).map(UUID::toString).toList();
    assertEquals(2, sessionInfoList.size());
    assertEquals(2, sessionIds.size());
    assertTrue(sessionIds.containsAll(List.of(session_1.getSessionId(), session_2.getSessionId())));
    assertFalse(sessionIds.contains(differentDevice.getSessionId()));
  }

  @Test
  void testDeleteAndNotify() {
    assertDoesNotThrow(() -> sessionService.deleteAndNotify(savedSessionId, StatusInfo.NETWORK_TERMINATED));
    verify(eventHubService, times(1)).sendEvent(any());
  }

  @ParameterizedTest
  @EnumSource(names = {"AVAILABLE", "UNAVAILABLE"})
  void testDeleteAndNotify_DeleteRequested(QosStatus qosStatus) {
    QosSession qosSession = qosSessionRepository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(qosSession);
    qosSession.setQosStatus(qosStatus);
    qosSessionRepository.save(qosSession);
    assertDoesNotThrow(() -> sessionService.deleteAndNotify(savedSessionId, StatusInfo.DELETE_REQUESTED));
    if (qosStatus == QosStatus.AVAILABLE) {
      verify(eventHubService, times(1)).sendEvent(any());
    } else {
      verify(eventHubService, times(0)).sendEvent(any());
    }
    verify(deleteApi, times(1)).scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(any(), any());
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testDeleteAndNotify_DeleteRequested_WithoutSubscriptionId(CapturedOutput output) {
    QosSession qosSession = qosSessionRepository.findBySubscriptionId(savedSubscriptionId).orElse(null);
    assertNotNull(qosSession);
    qosSession.setSubscriptionId(null);
    qosSessionRepository.save(qosSession);
    assertDoesNotThrow(() -> sessionService.deleteAndNotify(savedSessionId, StatusInfo.DELETE_REQUESTED));
    verify(deleteApi, times(0)).scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(any(), any());
    assertTrue(output.getAll()
        .contains("A corresponding network-subscription for this session does not exist - no network subscription-deletion performed"));
  }

  @Test
  void testDeleteSession_NotFound_404() {
    UUID uuid = UUID.randomUUID();
    when(qosSessionRepository.findBySessionId(any())).thenReturn(Optional.empty());
    QodApiException exception = assertThrows(QodApiException.class,
        () -> sessionService.deleteAndNotify(uuid.toString(), StatusInfo.DELETE_REQUESTED));
    assertTrue(exception.getMessage().contains("not found"));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  void testDeleteSession_ServiceUnavailable_Nef_503() {
    HttpClientErrorException httpClientErrorException = createHttpClientErrorException(503, "test error");
    doThrow(httpClientErrorException).when(deleteApi).scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(anyString(), any());
    var sessionInfo = createSession(createValidTestSession());
    QodApiException exception = assertThrows(QodApiException.class,
        () -> sessionService.deleteAndNotify(sessionInfo.getSessionId().toString(), StatusInfo.DELETE_REQUESTED));
    assertTrue(exception.getMessage().contains("test error"));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
  }


  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testDeleteSession_NotFound_Nef_404(CapturedOutput output) {
    HttpClientErrorException httpClientErrorException = createHttpClientErrorException(404, "test error");
    doThrow(httpClientErrorException).when(deleteApi).scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(anyString(), any());
    var sessionInfo = createSession(createValidTestSession());
    assertDoesNotThrow(() -> sessionService.deleteAndNotify(sessionInfo.getSessionId().toString(), StatusInfo.DELETE_REQUESTED));
    assertTrue(output.getAll().contains("Problem by calling NEF/SCEF (Possibly already deleted by NEF)"));
  }

  @Test
  void testExtendSession_Ok() {
    qosSessionTestData.setQosStatus(QosStatus.AVAILABLE);
    OffsetDateTime now = OffsetDateTime.now();
    qosSessionTestData.setStartedAt(now.format(ISO_DATE_TIME));
    qosSessionTestData.setExpiresAt(now.plusSeconds(qosSessionTestData.getDuration()).format(ISO_DATE_TIME));
    when(qosSessionRepository.findAllByClientId(any())).thenReturn(List.of(qosSessionTestData));
    assertDoesNotThrow(() -> sessionService.extendQosSession(UUID.fromString(savedSessionId), 40));
  }


  @Test
  void testExtendSession_InDeletingProcess_404() {
    qosSessionTestData.setScheduledForDeletion(true);
    qosSessionTestData.setQosStatus(QosStatus.AVAILABLE);
    when(qosSessionRepository.findAllByClientId(any())).thenReturn(List.of(qosSessionTestData));

    var qodApiException = assertThrows(QodApiException.class, () -> sessionService.extendQosSession(UUID.fromString(savedSessionId), 40));
    assertEquals("The Quality of Service (QoS) session has reached its expiration, and the deletion process is running.",
        qodApiException.getMessage());
    assertEquals(HttpStatus.NOT_FOUND, qodApiException.getHttpStatus());
  }

  @Test
  void testExtendSession_CurrentStatus_Requested_403() {
    qosSessionTestData.setQosStatus(QosStatus.REQUESTED);
    when(qosSessionRepository.findAllByClientId(any())).thenReturn(List.of(qosSessionTestData));

    QodApiException qodApiException = assertThrows(QodApiException.class, () -> sessionService.extendQosSession(
        UUID.fromString(savedSessionId), 600));
    assertEquals("Extending the session duration is not allowed in the current state REQUESTED."
            + " The session must be in the AVAILABLE state.",
        qodApiException.getMessage());
    assertEquals(HttpStatus.CONFLICT, qodApiException.getHttpStatus());
  }

  @Test
  void testExtendSession_SessionIsInDeletionProcess_404() {
    qosSessionTestData.setSessionId(savedSessionId);
    qosSessionTestData.setQosStatus(QosStatus.AVAILABLE);
    qosSessionTestData.setScheduledForDeletion(true);
    qosSessionTestData.setDuration(86300);
    when(qosSessionRepository.findAllByClientId(any())).thenReturn(List.of(qosSessionTestData));

    QodApiException qodApiException = assertThrows(QodApiException.class, () -> sessionService.extendQosSession(
        UUID.fromString(savedSessionId), 600));
    assertEquals("The Quality of Service (QoS) session has reached its expiration, and the deletion process is running.",
        qodApiException.getMessage());
    assertEquals(HttpStatus.NOT_FOUND, qodApiException.getHttpStatus());
  }


  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testExtendSessionToSessionLimit_Ok(CapturedOutput output) {
    qosSessionTestData.setDuration(PROFILE_MAX_DURATION - 1);
    qosSessionTestData.setQosStatus(QosStatus.AVAILABLE);
    qosSessionTestData.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC).format(ISO_DATE_TIME));
    when(qosSessionRepository.findAllByClientId(any())).thenReturn(List.of(qosSessionTestData));

    assertDoesNotThrow(() -> sessionService.extendQosSession(UUID.fromString(savedSessionId), PROFILE_MAX_DURATION + 2));
    assertTrue(output.getAll()
        .contains("New duration exceeds max duration of 86400 for profile <QOS_L>. Setting to max duration."));
  }

  @Test
  void testCreateExtendToLimit_SessionIsAlreadyAtMaxDuration_400() {
    qosSessionTestData.setDuration(PROFILE_MAX_DURATION);
    qosSessionTestData.setQosStatus(QosStatus.AVAILABLE);
    when(qosSessionRepository.findAllByClientId(any())).thenReturn(List.of(qosSessionTestData));

    QodApiException qodApiException = assertThrows(QodApiException.class, () -> sessionService.extendQosSession(
        UUID.fromString(savedSessionId), 600));
    assertEquals(HttpStatus.BAD_REQUEST, qodApiException.getHttpStatus());
    assertEquals(
        String.format("Session is already at max duration for QoS profile <%s>", qosSessionTestData.getQosProfile()),
        qodApiException.getMessage()
    );
  }

  @Test
  void testCreateExtendToLimit_CurrentStatus_Unavailable_Conflict() {
    qosSessionTestData.setDuration(82000);
    qosSessionTestData.setQosStatus(QosStatus.UNAVAILABLE);
    when(qosSessionRepository.findAllByClientId(any())).thenReturn(List.of(qosSessionTestData));

    QodApiException qodApiException = assertThrows(QodApiException.class, () -> sessionService.extendQosSession(
        UUID.fromString(savedSessionId), 600));
    assertEquals(HttpStatus.CONFLICT, qodApiException.getHttpStatus());
    assertEquals("Extending the session duration is not allowed in the current state UNAVAILABLE. "
            + "The session must be in the AVAILABLE state.",
        qodApiException.getMessage());
  }

  /**
   * Extend a session, expect not found response.
   */
  @Test
  void testExtendSession_NotFound_404() {
    var sessionId = UUID.randomUUID();
    QodApiException exception = assertThrows(QodApiException.class, () -> sessionService.extendQosSession(sessionId, 400));
    assertTrue(exception.getMessage().contains("The specified session does not exist"));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  void testGetExpiringQosSessions() {
    OffsetDateTime now = OffsetDateTime.now();

    var expiresSoon = createQosSessionTestData();
    expiresSoon.getDevice().getIpv4Address().setPublicAddress("127.0.0.1");
    expiresSoon.setQosStatus(QosStatus.AVAILABLE);
    expiresSoon.setStartedAt(now.format(ISO_DATE_TIME));
    expiresSoon.setExpiresAt(now.plusSeconds(5).format(ISO_DATE_TIME));

    final var expiresLater = createQosSessionTestData();
    expiresLater.getDevice().getIpv4Address().setPublicAddress("127.0.0.2");
    expiresLater.setQosStatus(QosStatus.AVAILABLE);
    expiresLater.setDuration(SECONDS_OF_A_DAY);
    expiresLater.setStartedAt(now.format(ISO_DATE_TIME));
    expiresLater.setExpiresAt(now.plusDays(1).format(ISO_DATE_TIME));

    final var alreadyScheduledForDeletionInPast = createQosSessionTestData();
    alreadyScheduledForDeletionInPast.getDevice().getIpv4Address().setPublicAddress("127.0.0.2");
    alreadyScheduledForDeletionInPast.setQosStatus(QosStatus.AVAILABLE);
    alreadyScheduledForDeletionInPast.setDuration(SECONDS_OF_A_DAY);
    alreadyScheduledForDeletionInPast.setStartedAt(now.minusDays(5).format(ISO_DATE_TIME));
    alreadyScheduledForDeletionInPast.setExpiresAt(now.minusSeconds(10).format(ISO_DATE_TIME));
    alreadyScheduledForDeletionInPast.setScheduledForDeletion(false);

    final var alreadyScheduledForDeletionInFuture = createQosSessionTestData();
    alreadyScheduledForDeletionInPast.getDevice().getIpv4Address().setPublicAddress("127.0.0.2");
    alreadyScheduledForDeletionInPast.setQosStatus(QosStatus.AVAILABLE);
    alreadyScheduledForDeletionInPast.setDuration(SECONDS_OF_A_DAY);
    alreadyScheduledForDeletionInPast.setStartedAt(now.format(ISO_DATE_TIME));
    alreadyScheduledForDeletionInPast.setExpiresAt(now.plusDays(10).format(ISO_DATE_TIME));
    alreadyScheduledForDeletionInPast.setScheduledForDeletion(true);

    when(qosSessionRepository.findAll()).thenReturn(
        List.of(expiresSoon, expiresLater, alreadyScheduledForDeletionInPast, alreadyScheduledForDeletionInFuture));

    List<QosSession> expiringSessions = assertDoesNotThrow(() -> sessionService.getExpiringQosSessions());
    assertEquals(1, expiringSessions.size());
    assertEquals(expiresSoon.getSessionId(), expiringSessions.getFirst().getSessionId());
  }

  private SessionInfo createSession(CreateSession createSession) {
    var sessionInfo = sessionService.createSession(createSession, true);
    assertNotNull(sessionInfo);
    return sessionInfo;
  }
}
