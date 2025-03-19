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


import com.camara.model.Device;
import com.camara.model.Device.DeviceIpv4Addr;

public class TokenTestData extends TestData {

  private static final String BEARER_PREFIX = "Bearer ";

  public static final String TEST_BEARER_TOKEN_NO_DEVICE = BEARER_PREFIX
      + "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICItQ2oyakVyejN0b1d0Y1FZVWtzRFB3aGFXZHQyS3AteWpZMTduRjBXdjZvIn0"
      + ".eyJleHAiOjE3MzMxNzU2ODQsImlhdCI6MTczMzE3NTM4NCwianRpIjoiNDE5MmIwZWUtMzg3Yi00OT"
      + "QxLWJlYzctNGFhMmM0NDEzZGRhIiwiaXNzIjoiaHR0cHM6Ly9wbGF5Z3JvdW5kLnNwYWNlZ2F0ZS50ZWxla29tLmRlL2F1dGgvcmVhbG1zL2RlZmF1bHQiLCJzdWIiOi"
      + "I2M2RmOTRmNC0yYWZkLTRiOWMtOGZjZS0yNjgwNThjN2IxOTMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJtYWNlLS1hcGktZGV2LS1rcmFrb3ctZGV2aWNlLWxvY2F0aW9"
      + "uLXNlbmYiLCJhY3IiOiIxIiwic2NvcGUiOiJjbGllbnQtb3JpZ2luIHByb2ZpbGUgZW1haWwiLCJjbGllbnRJZCI6Im1hY2UtLWFwaS1kZXYtLWtyYWtvdy1kZXZpY2U"
      + "tbG9jYXRpb24tc2VuZiIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiY2xpZW50SG9zdCI6IjEwMC4xMDIuMTQ0Ljc4Iiwib3JpZ2luWm9uZSI6InNwYWNlIiwicHJlZmV"
      + "ycmVkX3VzZXJuYW1lIjoic2VydmljZS1hY2NvdW50LW1hY2UtLWFwaS1kZXYtLWtyYWtvdy1kZXZpY2UtbG9jYXRpb24tc2VuZiIsImNsaWVudEFkZHJlc3MiOiIxMDA"
      + "uMTAyLjE0NC43OCIsIm9yaWdpblN0YXJnYXRlIjoiaHR0cHM6Ly9wbGF5Z3JvdW5kLnNwYWNlZ2F0ZS50ZWxla29tLmRlIn0"
      + ".UastGING50kZCqdTz611x3HGnSSYVpCDq4qGhwZEZrIGyw0CC_Oex1idXIFJQ9U6i0v1p67019fbXljazZanEVMA6tWg9rO-uXq66qg19t"
      + "-RQIXFqn52RlV1HnQJ3dkJvde3630qcftNB4PJG7EzdH14wzhtwJ5Rrl-wVkLpRM4CEaf1Whxb0L1PTZ6SsSezim5vQ2GWo9NYMG"
      + "-pbZAb7KAZXqETkpCV0iJWs5HxDiPlUXUIKb377nS1L9uXUyNAIg_-0WC07o6tS7YQkxjUEvRnl3OsS6cVqCbjOJ-KI0UbM8zHWQv-_QwK"
      + "-xJz4XRe66O3a2FqwJt46kiBt5QcPQ";

  public static final String TEST_BEARER_TOKEN_WITH_MSISDN = BEARER_PREFIX
      + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
      + ".eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJwaG9uZU51bWJlciI6IjE5OC41MS4xMDAuMSJ9"
      + ".aFJras7iizYq2Sng8CzI7L6sa5e1f4cNaJH-eYqnnuM";

  public static final String TEST_BEARER_TOKEN_WITH_IPV4 = BEARER_PREFIX
      + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
      + ".eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJpcHY0QWRkcmVzc19wdWJsaWMiOiIxOTguNTEuMTAwLjEiLCJpc"
      + "HY0QWRkcmVzc19wcml2YXRlIjoiMTAuMC4wLjAifQ"
      + ".Gbegf-xnjlZk13GGMWLQzvTPQSKW1WNAW021b9GbgBE";
  public static final String TEST_BEARER_TOKEN = BEARER_PREFIX
      + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRJZCI6ImNsaWVudC1pZCJ9.qeV1BSRxUBZ6SGDS4fl37udqzf-ghdIpd5KnBB4QzH0";

  public static final String INVALID_BEARER_TOKEN = "Bearer invalid";
  public static final String TEST_CLIENT_ID = "client-id";
  public static final String TEST_IPV4_PUBLIC_FOR_TOKEN_TEST = "198.51.100.1";

  /**
   * Creates a {@link Device}.
   *
   * @return the created device
   */
  public static Device createDevice() {
    DeviceIpv4Addr deviceIpv4Addr = new DeviceIpv4Addr();
    deviceIpv4Addr.setPublicAddress(SessionsTestData.TEST_DEVICE_IPV4_ADDRESS);
    return Device.builder()
        .ipv4Address(deviceIpv4Addr)
        .build();
  }
}
