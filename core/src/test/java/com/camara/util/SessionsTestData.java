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

package com.camara.util;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import com.camara.entity.QosSession;
import com.camara.model.SupportedQosProfiles;
import com.camara.network.api.model.AsSessionWithQoSSubscription;
import com.camara.quality_on_demand.api.model.ApplicationServer;
import com.camara.quality_on_demand.api.model.CreateSession;
import com.camara.quality_on_demand.api.model.Device;
import com.camara.quality_on_demand.api.model.DeviceIpv4Addr;
import com.camara.quality_on_demand.api.model.PortsSpec;
import com.camara.quality_on_demand.api.model.PortsSpecRangesInner;
import com.camara.quality_on_demand.api.model.QosStatus;
import com.camara.quality_on_demand.api.model.RetrieveSessionsInput;
import com.camara.quality_on_demand.api.model.SessionInfo;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * This test data class provides useful session test data.
 */
public class SessionsTestData extends TestData {

  public static final String QOD_SESSIONS_URI = "/quality-on-demand/v0.11/sessions";
  public static final String QOD_SESSIONS_RETRIEVE_URI = "/quality-on-demand/v0.11/retrieve-sessions";
  public static final int DURATION_DEFAULT = 10;
  public static final String SESSION_UUID = "000ab9f5-26e8-48b9-a56e-52ecdeaa9172";
  public static final String TEST_DEVICE_IPV4_ADDRESS = "172.24.11.4";
  public static final String TEST_DEVICE_IPV4_ADDRESS_UNKNOWN = "172.24.11.7";
  public static final String TEST_APP_SERVER_IPV4_ADDRESS = "200.24.24.2";
  public static final String TEST_SINK = "https://application-server.com/notifications";

  /**
   * Creates test retrieve session.
   *
   * @return the {@link RetrieveSessionsInput}
   */
  public static RetrieveSessionsInput createRetrieveSessionsRequest() {
    return new RetrieveSessionsInput()
        .device(createDeviceWithIpv4(TEST_DEVICE_IPV4_ADDRESS));
  }

  /**
   * Creates test session with specified qosProfile.
   *
   * @param qosProfile - chosen QosProfile
   * @return the {@link CreateSession}
   */
  public static CreateSession createTestSession(SupportedQosProfiles qosProfile) {
    CreateSession session = createDefaultTestSession();
    session.qosProfile(qosProfile.name());
    return session;
  }

  /**
   * Creates test session with specified duration.
   *
   * @param duration {@link Integer} value of duration in ms
   * @return the {@link CreateSession}
   */
  public static CreateSession createTestSession(Integer duration) {
    CreateSession session = createDefaultTestSession();
    session.duration(duration);
    return session;
  }

  /**
   * Creates a test session with specified ports.
   *
   * @param portsRangeStart - beginning of the port range
   * @param portRangeEnd    - end of the port range
   * @param additionalPort  - additional single port outside the range
   * @return the {@link CreateSession}
   */
  public static CreateSession createTestSession(int portsRangeStart, int portRangeEnd, int additionalPort) {
    CreateSession session = createDefaultTestSession();
    session.devicePorts(new PortsSpec()
        .ranges(Collections.singletonList(new PortsSpecRangesInner()
            .from(portsRangeStart)
            .to(portRangeEnd)))
        .ports(Collections.singletonList(additionalPort)));
    return session;
  }

  public static CreateSession createValidTestSession() {
    return createDefaultTestSession();
  }


  /**
   * Create a SessionInfo object sample.
   *
   * @return the {@link SessionInfo}
   */
  public static SessionInfo createSessionInfoSample() {
    return new SessionInfo()
        .sessionId(UUID.fromString(SESSION_UUID))
        .duration(60)
        .device(createDeviceWithIpv4("198.51.100.1"))
        .applicationServer(createApplicationServerWithIpv4("198.51.100.1"))
        .devicePorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .applicationServerPorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .qosProfile(SupportedQosProfiles.QOS_E.name())
        .sink(TEST_SINK);
  }

  private static Device createDeviceWithIpv4(String ipv4) {
    return new Device()
        .ipv4Address(new DeviceIpv4Addr().publicAddress(ipv4)
        );
  }

  private static ApplicationServer createApplicationServerWithIpv4(String ipv4) {
    return new ApplicationServer()
        .ipv4Address(ipv4);
  }

  /**
   * Creates a {@link QosSession} for test usage.
   *
   * @return the created {@link QosSession}
   */
  public static QosSession createQosSessionTestData() {
    return QosSession.builder()
        .id(UUID.randomUUID().toString())
        .sessionId(UUID.randomUUID().toString())
        .subscriptionId(UUID.randomUUID().toString())
        .duration(DURATION_DEFAULT)
        .device(createDeviceWithIpv4("198.51.100.1"))
        .applicationServer(createApplicationServerWithIpv4("198.51.100.1"))
        .devicePorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .applicationServerPorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .qosProfile(SupportedQosProfiles.QOS_L.name())
        .qosStatus(QosStatus.REQUESTED)
        .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).format(ISO_DATE_TIME))
        .subscriptionId("subscrId123")
        .sink(TEST_SINK)
        .clientId(TokenTestData.TEST_CLIENT_ID)
        .build();
  }

  /**
   * Creates a {@link QosSession} for test usage.
   *
   * @return the created {@link QosSession}
   */
  public static QosSession createQosSessionTestData(UUID sessionId) {
    return QosSession.builder()
        .sessionId(sessionId.toString())
        .duration(DURATION_DEFAULT)
        .device(createDeviceWithIpv4("198.51.100.1"))
        .applicationServer(createApplicationServerWithIpv4("198.51.100.1"))
        .devicePorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .applicationServerPorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .qosProfile(SupportedQosProfiles.QOS_L.name())
        .qosStatus(QosStatus.REQUESTED)
        .subscriptionId("subscrId123")
        .sink(TEST_SINK)
        .build();
  }

  /**
   * Create a sample response from NEF including the subscription-Id.
   *
   * @return {@link AsSessionWithQoSSubscription}
   */
  public static AsSessionWithQoSSubscription createNefSubscriptionResponse() {
    return new AsSessionWithQoSSubscription().self(
        "https://foo.com/subscriptions/" + UUID.randomUUID());
  }

  /**
   * Create a sample response from NEF without the subscription ID.
   *
   * @return {@link AsSessionWithQoSSubscription}
   */
  public static AsSessionWithQoSSubscription createNefSubscriptionResponseWithoutSubscriptionId() {
    return new AsSessionWithQoSSubscription().self(null);
  }

  /**
   * Creates the default and valid test session.
   *
   * @return the {@link CreateSession}
   */
  private static CreateSession createDefaultTestSession() {
    return new CreateSession()
        .device(createDeviceWithIpv4(TEST_DEVICE_IPV4_ADDRESS))
        .applicationServer(createApplicationServerWithIpv4(TEST_APP_SERVER_IPV4_ADDRESS))
        .duration(DURATION_DEFAULT)
        .devicePorts(null)
        .qosProfile(SupportedQosProfiles.QOS_E.name())
        .sink(TEST_SINK);
  }

  /**
   * Creates the default and valid test session with Unknown IPv4 Address.
   *
   * @return the {@link CreateSession}
   */
  public static CreateSession createDefaultTestSessionWithUnknownIpv4() {
    return new CreateSession()
        .device(createDeviceWithIpv4(TEST_DEVICE_IPV4_ADDRESS_UNKNOWN))
        .applicationServer(createApplicationServerWithIpv4(TEST_APP_SERVER_IPV4_ADDRESS))
        .duration(DURATION_DEFAULT)
        .devicePorts(null)
        .qosProfile(SupportedQosProfiles.QOS_E.name())
        .sink(TEST_SINK);
  }

  /**
   * Creates a test {@link SessionInfo}.
   *
   * @return {@link SessionInfo}
   */
  public static SessionInfo createTestSessionInfo() {
    return createTestSessionInfo(UUID.randomUUID());
  }

  /**
   * Creates a test {@link SessionInfo} based on a sessionId.
   *
   * @param sessionId the sessionId for this session info
   * @return {@link SessionInfo}
   */
  public static SessionInfo createTestSessionInfo(UUID sessionId) {
    return new SessionInfo()
        .sessionId(sessionId)
        .device(new Device().ipv4Address(new DeviceIpv4Addr().publicAddress(TEST_DEVICE_IPV4_ADDRESS)))
        .duration(DURATION_DEFAULT)
        .sink(TEST_SINK)
        .qosStatus(QosStatus.AVAILABLE);
  }
}
