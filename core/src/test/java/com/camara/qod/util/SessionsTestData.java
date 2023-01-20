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

package com.camara.qod.util;

import com.camara.qod.api.model.AsId;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.PortsSpec;
import com.camara.qod.api.model.PortsSpecRanges;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.api.model.UeId;
import com.camara.qod.entity.H2QosSession;
import com.camara.qod.entity.RedisQosSession;
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
  public static final String NOTIFICATION_URI = "/3gpp-as-session-with-qos/v1/notifications";
  public static final int DURATION_DEFAULT = 2;
  public static final String SESSION_UUID = "000ab9f5-26e8-48b9-a56e-52ecdeaa9172";
  public static final String AVAILABILITY_SERVICE_URI = "/api/v1/sessions";

  public static CreateSession createTestSession(QosProfile qosProfile) {
    return createTestSession(qosProfile, new AsId().ipv4addr("200.24.24.2"));
  }

  public static CreateSession createTestSession(QosProfile qosProfile, AsId asId) {
    return createTestSession(qosProfile, asId, null, DURATION_DEFAULT);
  }


  public static CreateSession createTestSession(Integer duration) {
    return createTestSession(QosProfile.E, new AsId().ipv4addr("200.24.24.2"), null, duration);
  }

  /**
   * Creates a test session by params.
   */
  public static CreateSession createTestSession(
      QosProfile qosProfile, AsId asId, PortsSpec uePorts, Integer duration) {
    return new CreateSession()
        .ueId(new UeId().ipv4addr("172.24.11.4"))
        .asId(asId)
        .duration(duration)
        .uePorts(uePorts)
        .qos(qosProfile)
        .notificationUri(URI.create("https://example.com"))
        .notificationAuthToken("12345");
  }

  /**
   * Creates test session request content.
   */
  public static String getTestSessionRequest() {
    return "{"
        + "\"duration\": 50,"
        + "\"ueId\": {"
        + "\"externalId\": \"123456789@domain.com\","
        + "\"msisdn\": \"123456789\","
        + "\"ipv4addr\": \"192.168.0.0/24\""
        + "},"
        + "\"asId\": {"
        + "\"ipv4addr\": \"192.168.0.0/24\"},"
        + "\"uePorts\": {"
        + "\"ranges\": [{"
        + "\"from\": 5010,"
        + "\"to\": 5020}],"
        + "\"ports\": [5060, 5070]"
        + "},"
        + "\"asPorts\": {"
        + "\"ranges\": [{"
        + "\"from\": 5010,"
        + "\"to\": 5020}],"
        + "\"ports\": [5060,5070]},"
        + "\"qos\": \"QOS_E\","
        + "\"notificationUri\": \"https://application-server.com/notifications\","
        + "\"notificationAuthToken\": \"c8974e592c2fa383d4a3960714\"\n}";
  }

  /**
   * Creates test session request content, with invalid network data, e.g. -/16 subnet.
   */
  public static String getTestSessionNetworkInvalid() {
    return "{"
        + "\"duration\": 50,"
        + "\"ueId\": {"
        + "\"externalId\": \"123456789@domain.com\","
        + "\"msisdn\": \"123456789\","
        + "\"ipv4addr\": \"198.51.100.1/16\""
        + "},"
        + "\"asId\": {"
        + "\"ipv4addr\": \"198.51.100.1/18\"},"
        + "\"uePorts\": {"
        + "\"ranges\": [{"
        + "\"from\": 5010,"
        + "\"to\": 5020}],"
        + "\"ports\": [5060, 5070]"
        + "},"
        + "\"asPorts\": {"
        + "\"ranges\": [{"
        + "\"from\": 5010,"
        + "\"to\": 5020}],"
        + "\"ports\": [5060,5070]},"
        + "\"qos\": \"QOS_E\","
        + "\"notificationUri\": \"https://application-server.com/notifications\","
        + "\"notificationAuthToken\": \"c8974e592c2fa383d4a3960714\"\n}";
  }

  /**
   * Creates test session request content, with invalid address data, e.g. 1987.51.100.1 IP.
   */
  public static String getTestSessionAddrInvalid() {
    return "{"
        + "\"duration\": 50,"
        + "\"ueId\": {"
        + "\"externalId\": \"123456789@domain.com\","
        + "\"msisdn\": \"123456789\","
        + "\"ipv4addr\": \"1923.168.0.0/24\""
        + "},"
        + "\"asId\": {"
        + "\"ipv4addr\": \"192.168.0.0/24\"},"
        + "\"uePorts\": {"
        + "\"ranges\": [{"
        + "\"from\": 5010,"
        + "\"to\": 5020}],"
        + "\"ports\": [5060, 5070]"
        + "},"
        + "\"asPorts\": {"
        + "\"ranges\": [{"
        + "\"from\": 5010,"
        + "\"to\": 5020}],"
        + "\"ports\": [5060,5070]},"
        + "\"qos\": \"QOS_E\","
        + "\"notificationUri\": \"https://application-server.com/notifications\","
        + "\"notificationAuthToken\": \"c8974e592c2fa383d4a3960714\"\n}";
  }

  /**
   * Create a SessionInfo object sample.
   */
  public static SessionInfo createSessionInfoSample() throws Exception {
    SessionInfo info = new SessionInfo();
    info.setId(UUID.fromString(SESSION_UUID));
    info.setStartedAt(1665730582L);
    info.setExpiresAt(1665730642L);
    info.setMessages(Collections.emptyList());
    info.setDuration(60);
    info.ueId(new UeId().ipv4addr("198.51.100.1"));
    info.asId(new AsId().ipv4addr("198.51.100.1"));
    info.uePorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRanges().from(5010).to(5020))));
    info.asPorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRanges().from(5010).to(5020))));
    info.setQos(QosProfile.E);
    info.setNotificationUri(new URI("http://application-server.com/notifications"));
    info.setNotificationAuthToken("c8974e592c2fa383d4a3960714");

    return info;
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
        .ueId(new UeId().ipv4addr("198.51.100.1"))
        .asId(new AsId().ipv4addr("198.51.100.1"))
        .uePorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRanges().from(5010).to(5020))))
        .asPorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRanges().from(5010).to(5020))))
        .qos(QosProfile.L)
        .subscriptionId("subscrId123")
        .notificationUri(new URI("http://application-server.com/notifications"))
        .notificationAuthToken("c8974e592c2fa383d4a3960714")
        .expirationLockUntil(0)
        .bookkeeperId(null)
        .build();
  }

  /**
   * Creates a {@link RedisQosSession} for test usage.
   *
   * @return the created {@link RedisQosSession}
   * @throws URISyntaxException when there is no good URI
   */
  public static RedisQosSession getRedisQosSessionTestData() throws URISyntaxException {
    return RedisQosSession.builder()
        .id(UUID.randomUUID())
        .startedAt(1665730582L)
        .expiresAt(1665730642L)
        .duration(DURATION_DEFAULT)
        .ueId(new UeId().ipv4addr("198.51.100.1"))
        .asId(new AsId().ipv4addr("198.51.100.1"))
        .uePorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRanges().from(5010).to(5020))))
        .asPorts(new PortsSpec().ports(List.of(5021, 5022)).ranges(List.of(new PortsSpecRanges().from(5010).to(5020))))
        .qos(QosProfile.L)
        .subscriptionId("subscrId123")
        .notificationUri(new URI("http://application-server.com/notifications"))
        .notificationAuthToken("c8974e592c2fa383d4a3960714")
        .expirationLockUntil(0)
        .bookkeeperId(null)
        .build();
  }

}
