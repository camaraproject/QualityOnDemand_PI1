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

package com.camara.qod.util;

import com.camara.network.api.model.AsSessionWithQoSSubscription;
import com.camara.qod.api.model.ApplicationServer;
import com.camara.qod.api.model.BaseSessionInfoWebhook;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.Device;
import com.camara.qod.api.model.DeviceIpv4Addr;
import com.camara.qod.api.model.PortsSpec;
import com.camara.qod.api.model.PortsSpecRangesInner;
import com.camara.qod.api.model.QosStatus;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.entity.H2QosSession;
import com.camara.qod.model.QosSession;
import com.camara.qod.model.SupportedQosProfiles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * This test data class provides useful session test data.
 */
public class SessionsTestData extends TestData {

  public static final String SESSION_URI = "/qod/v0/sessions";
  public static final int DURATION_DEFAULT = 10;
  public static final String SESSION_UUID = "000ab9f5-26e8-48b9-a56e-52ecdeaa9172";
  public static final String TEST_DEVICE_IPV4_ADDRESS = "172.24.11.4";
  public static final String TEST_DEVICE_IPV4_ADDRESS_UNKNOWN = "172.24.11.7";
  public static final String TEST_DEVICE_IPV4_ADDRESS_WITH_NETWORK_SEGMENT = "172.24.11.4/12";
  public static final String TEST_APP_SERVER_IPV4_ADDRESS = "200.24.24.2";
  public static final String TEST_INVALID_AS_IPV4_ADDRESS = "invalid";

  public static final URI TEST_WEBHOOK_NOTIFICATION_URL = URI.create("https://example.com");

  public static final String SESSION_XML_REQUEST = """
      <?xml version="1.0" encoding="UTF-8" ?>
      <root>
          <duration>7200</duration>
          <ueId>
              <externalId>123456789@domain.com</externalId>
              <msisdn>123456789</msisdn>
              <ipv4addr>192.168.0.0/24</ipv4addr>
          </ueId>
          <asId>
              <ipv4addr>192.168.0.0/24</ipv4addr>
          </asId>
          <uePorts>
              <ranges>
                  <from>5010</from>
                  <to>5020</to>
              </ranges>
              <ports>5060</ports>
              <ports>5070</ports>
          </uePorts>
          <asPorts>
              <ranges>
                  <from>5010</from>
                  <to>5020</to>
              </ranges>
              <ports>5060</ports>
              <ports>5070</ports>
          </asPorts>
          <qos>QOS_E</qos>
          <notificationUri>http://127.0.0.1:8000/notifications</notificationUri>
          <notificationAuthToken>c8974e592c2fa383d4a3960714</notificationAuthToken>
      </root>
      """;


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
   * @param portsRangeStart - beginning of the ports range
   * @param portRangeEnd    - end of the ports range
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

  public static CreateSession createValidTestSessionWithGivenDuration(Integer duration) {
    return createDefaultTestSessionWithDuration(duration);
  }


  /**
   * Creates test session with invalid IPv4 (with subnet).
   *
   * @return the {@link CreateSession}
   */
  public static CreateSession createTestSessionWithInvalidAppServerNetwork() {
    CreateSession session = createDefaultTestSession();
    session.getApplicationServer().ipv4Address(TEST_INVALID_AS_IPV4_ADDRESS);
    return session;
  }

  /**
   * Creates test session with IPv4 in invalid format.
   *
   * @return the {@link CreateSession}
   */
  public static CreateSession createTestSessionWithWrongDeviceIpv4Format() {
    CreateSession session = createDefaultTestSession();
    session.setDevice(createDeviceWithIpv4("invalid"));
    return session;
  }

  /**
   * Create a SessionInfo object sample.
   *
   * @return the {@link SessionInfo}
   */
  public static SessionInfo createSessionInfoSample() throws Exception {
    return new SessionInfo()
        .sessionId(UUID.fromString(SESSION_UUID))
        .startedAt(1665730582L)
        .expiresAt(1665730642L)
        .messages(Collections.emptyList())
        .duration(60)
        .device(createDeviceWithIpv4("198.51.100.1"))
        .applicationServer(createApplicationServerWithIpv4("198.51.100.1"))
        .devicePorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .applicationServerPorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .qosProfile(SupportedQosProfiles.QOS_E.name())
        .webhook(
            new BaseSessionInfoWebhook()
                .notificationUrl(new URI("https://application-server.com/notifications"))
                .notificationAuthToken("c8974e592c2fa383d4a3960714")
        );
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
   * Creates a {@link H2QosSession} for test usage.
   *
   * @return the created {@link H2QosSession}
   * @throws URISyntaxException when there is no good URI
   */
  public static H2QosSession getH2QosSessionTestData() throws URISyntaxException {
    return H2QosSession.builder()
        .id(UUID.randomUUID())
        .startedAt(1665730582L)
        .expiresAt(1665730642L)
        .duration(DURATION_DEFAULT)
        .device(createDeviceWithIpv4("198.51.100.1"))
        .applicationServer(createApplicationServerWithIpv4("198.51.100.1"))
        .devicePorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .applicationServerPorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .qosProfile(SupportedQosProfiles.QOS_L.name())
        .qosStatus(QosStatus.REQUESTED)
        .subscriptionId("subscrId123")
        .notificationUrl(new URI("https://application-server.com/notifications"))
        .notificationAuthToken("c8974e592c2fa383d4a3960714")
        .expirationLockUntil(0)
        .bookkeeperId(null)
        .build();
  }

  /**
   * Creates a {@link H2QosSession} for test usage.
   *
   * @return the created {@link H2QosSession}
   * @throws URISyntaxException when there is no good URI
   */
  public static QosSession createQosSessionTestData(UUID sessionId) throws URISyntaxException {
    return QosSession.builder()
        .sessionId(sessionId)
        .startedAt(1665730582L)
        .expiresAt(1665730642L)
        .duration(DURATION_DEFAULT)
        .device(createDeviceWithIpv4("198.51.100.1"))
        .applicationServer(createApplicationServerWithIpv4("198.51.100.1"))
        .devicePorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .applicationServerPorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRangesInner().from(5010).to(5020))))
        .qosProfile(SupportedQosProfiles.QOS_L.name())
        .qosStatus(QosStatus.REQUESTED)
        .subscriptionId("subscrId123")
        .notificationUrl(new URI("https://application-server.com/notifications"))
        .notificationAuthToken("c8974e592c2fa383d4a3960714")
        .expirationLockUntil(0)
        .bookkeeperId(null)
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
        .webhook(
            new BaseSessionInfoWebhook()
                .notificationAuthToken("1234567890987654321012345")
                .notificationUrl(TEST_WEBHOOK_NOTIFICATION_URL)
        );
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
        .webhook(
            new BaseSessionInfoWebhook()
                .notificationAuthToken("1234567890987654321012345")
                .notificationUrl(TEST_WEBHOOK_NOTIFICATION_URL)
        );
  }

  /**
   * Creates the default and valid test session by given duration.
   *
   * @return the {@link CreateSession}
   */
  private static CreateSession createDefaultTestSessionWithDuration(Integer duration) {
    return new CreateSession()
        .device(createDeviceWithIpv4(TEST_DEVICE_IPV4_ADDRESS))
        .applicationServer(createApplicationServerWithIpv4(TEST_APP_SERVER_IPV4_ADDRESS))
        .duration(duration)
        .devicePorts(null)
        .qosProfile(SupportedQosProfiles.QOS_E.name())
        .webhook(
            new BaseSessionInfoWebhook()
                .notificationAuthToken("1234567890987654321012345")
                .notificationUrl(TEST_WEBHOOK_NOTIFICATION_URL)
        );
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
        .webhook(new BaseSessionInfoWebhook()
            .notificationUrl(TEST_WEBHOOK_NOTIFICATION_URL))
        .qosStatus(QosStatus.AVAILABLE);
  }
}
