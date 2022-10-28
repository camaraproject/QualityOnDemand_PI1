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

package com.camara.qod.util;

import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.Protocol;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.SessionInfo;
import java.net.URI;
import java.util.Collections;
import java.util.UUID;

/**
 * This test data class provides useful session test data.
 */
public class SessionsTestData extends TestData {

  public static final String SESSION_URI = "/qod-api/v0/sessions";
  public static final String AVAILABILITY_URI = "/qod-api/v0/check-qos-availability";
  public static final String NOTIFICATION_URI = "/3gpp-as-session-with-qos/v1/notifications";
  public static final int DURATION_DEFAULT = 2;
  public static final String SESSION_UUID = "000ab9f5-26e8-48b9-a56e-52ecdeaa9172";

  public static CreateSession createTestSession(QosProfile qosProfile) {
    return createTestSession(qosProfile, "200.24.24.2");
  }

  public static CreateSession createTestSession(QosProfile qosProfile, String asAddr) {
    return createTestSession(qosProfile, asAddr, null, DURATION_DEFAULT);
  }

  public static CreateSession createTestSession(
      QosProfile qosProfile, String asAddr, String uePorts, Integer duration) {
    return createTestSession(qosProfile, asAddr, uePorts, duration, Protocol.ANY);
  }

  public static CreateSession createTestSession(Integer duration) {
    return createTestSession(QosProfile.LOW_LATENCY, "200.24.24.2", null, duration);
  }

  /**
   * Creates a test session by params.
   */
  public static CreateSession createTestSession(
      QosProfile qosProfile, String asAddr, String uePorts, Integer duration, Protocol protocolIn) {
    return new CreateSession()
        .ueAddr("172.24.11.4")
        .asAddr(asAddr)
        .duration(duration)
        .uePorts(uePorts)
        .protocolIn(protocolIn)
        .protocolOut(Protocol.ANY)
        .qos(qosProfile)
        .notificationUri(URI.create("https://example.com"))
        .notificationAuthToken("12345");
  }

  /**
   * Creates test session request content.
   */
  public static String getTestSessionRequest() {
    return "{"
        + "  \"duration\": 60,"
        + "  \"ueAddr\": \"198.51.100.1\","
        + "  \"asAddr\": \"198.51.100.1\","
        + "  \"uePorts\": \"5010-5020,5021,5022\","
        + "  \"asPorts\": \"5010-5020,5021,5022\","
        + "  \"protocolIn\": \"TCP\","
        + "  \"protocolOut\": \"TCP\","
        + "  \"qos\": \"LOW_LATENCY\","
        + "  \"notificationUri\": \"https://application-server.com/notifications\","
        + "  \"notificationAuthToken\": \"c8974e592c2fa383d4a3960714\""
        + "}";
  }

  /**
   * Creates test session request content, with invalid network data, e.g. -/16 subnet.
   */
  public static String getTestSessionNetworkInvalid() {
    return "{"
        + "  \"duration\": 60,"
        + "  \"ueAddr\": \"198.51.100.1/16\","
        + "  \"asAddr\": \"198.51.100.1/18\","
        + "  \"uePorts\": \"5010-5020,5021,5022\","
        + "  \"asPorts\": \"5010-5020,5021,5022\","
        + "  \"protocolIn\": \"TCP\","
        + "  \"protocolOut\": \"TCP\","
        + "  \"qos\": \"LOW_LATENCY\","
        + "  \"notificationUri\": \"https://application-server.com/notifications\","
        + "  \"notificationAuthToken\": \"c8974e592c2fa383d4a3960714\""
        + "}";
  }

  /**
   * Creates test session request content, with invalid address data, e.g. 1987.51.100.1 IP.
   */
  public static String getTestSessionAddrInvalid() {
    return "{"
        + "  \"duration\": 60,"
        + "  \"ueAddr\": \"1987.51.100.1\","
        + "  \"asAddr\": \"198.51.100.1\","
        + "  \"uePorts\": \"5010-5020,5021,5022\","
        + "  \"asPorts\": \"5010-5020,5021,5022\","
        + "  \"protocolIn\": \"TCP\","
        + "  \"protocolOut\": \"TCP\","
        + "  \"qos\": \"LOW_LATENCY\","
        + "  \"notificationUri\": \"https://application-server.com/notifications\","
        + "  \"notificationAuthToken\": \"c8974e592c2fa383d4a3960714\""
        + "}";
  }

  /**
   * Creates test session request content, with invalid protocol data.
   */
  public static String getTestSessionProtocolInvalid() {
    return "{"
        + "  \"duration\": 60,"
        + "  \"ueAddr\": \"1987.51.100.1/16\","
        + "  \"asAddr\": \"198.51.100.1/18\","
        + "  \"uePorts\": \"5010-5020,5021,5022\","
        + "  \"asPorts\": \"5010-5020,5021,5022\","
        + "  \"protocolIn\": \"What?\","
        + "  \"protocolOut\": \"TCP\","
        + "  \"qos\": \"LOW_LATENCY\","
        + "  \"notificationUri\": \"https://application-server.com/notifications\","
        + "  \"notificationAuthToken\": \"c8974e592c2fa383d4a3960714\""
        + "}";
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
    info.setUeAddr("198.51.100.1");
    info.setAsAddr("198.51.100.1");
    info.setUePorts("5010 - 5020, 5021, 5022");
    info.setAsPorts("5010 - 5020, 5021, 5022");
    info.setProtocolIn(Protocol.TCP);
    info.setProtocolOut(Protocol.TCP);
    info.setQos(QosProfile.LOW_LATENCY);
    info.setNotificationUri(new URI("http://application-server.com/notifications"));
    info.setNotificationAuthToken("c8974e592c2fa383d4a3960714");

    return info;
  }

  /**
   * Create a notification request sample.
   */
  public static String getNotificationRequest() {
    return "{"
        + "  \"transaction\": \"123\","
        + "  \"eventReports\": ["
        + "      {"
        + "          \"event\": \"SESSION_TERMINATION\""
        + "      }"
        + "  ]"
        + "}";
  }

}
