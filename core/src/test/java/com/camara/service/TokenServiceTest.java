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

import static com.camara.util.TestData.mockRequestWithToken;
import static com.camara.util.TokenTestData.INVALID_BEARER_TOKEN;
import static com.camara.util.TokenTestData.TEST_BEARER_TOKEN;
import static com.camara.util.TokenTestData.TEST_BEARER_TOKEN_NO_DEVICE;
import static com.camara.util.TokenTestData.TEST_BEARER_TOKEN_WITH_IPV4;
import static com.camara.util.TokenTestData.TEST_BEARER_TOKEN_WITH_MSISDN;
import static com.camara.util.TokenTestData.TEST_CLIENT_ID;
import static com.camara.util.TokenTestData.TEST_IPV4_PUBLIC_FOR_TOKEN_TEST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.camara.config.QodConfig;
import com.camara.model.Device;
import com.camara.model.Device.DeviceIpv4Addr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
class TokenServiceTest {

  @InjectMocks
  private TokenService tokenService;

  @Mock
  private QodConfig qodConfig;

  @BeforeEach
  void setUp() {
    lenient().when(qodConfig.isAllowAnonymousClients()).thenReturn(false);
  }

  @Test
  void testRetrieveClientId_Ok() {
    mockRequestWithToken(TEST_BEARER_TOKEN);
    var clientId = tokenService.retrieveClientId();
    assertEquals(TEST_CLIENT_ID, clientId);
  }

  @Test
  void testRetrieveClientId_NoClientIdPresent_Null_Ok() {
    mockRequestWithToken(TEST_BEARER_TOKEN_WITH_IPV4);
    var clientId = tokenService.retrieveClientId();
    assertNull(clientId);
  }

  @Test
  void testRetrieveClientId_NoClientIdPresent_AnonymousClients_Ok() {
    when(qodConfig.isAllowAnonymousClients()).thenReturn(true);
    mockRequestWithToken(TEST_BEARER_TOKEN_WITH_IPV4);
    var clientId = tokenService.retrieveClientId();
    assertEquals("anonymous", clientId);
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testRetrieveDevice_WithoutDevice_Ok(CapturedOutput output) {
    mockRequestWithToken(TEST_BEARER_TOKEN_NO_DEVICE);
    Device device = tokenService.retrieveDevice();
    assertNull(device);
    assertTrue(output.getAll().contains("No device contained in access-token"));
  }

  @Test
  void testRetrieveDevice_WithMsisdn_NotSupported_Null_Ok() {
    mockRequestWithToken(TEST_BEARER_TOKEN_WITH_MSISDN);
    var device = assertDoesNotThrow(() -> tokenService.retrieveDevice());
    assertNull(device);
  }

  @Test
  void testRetrieveDevice_WithIpv4Only_Ok() {
    mockRequestWithToken(TEST_BEARER_TOKEN_WITH_IPV4);
    var device = assertDoesNotThrow(() -> tokenService.retrieveDevice());
    DeviceIpv4Addr ipv4Address = device.getIpv4Address();
    assertNull(device.getPhoneNumber());
    assertNotNull(ipv4Address);
    assertEquals(TEST_IPV4_PUBLIC_FOR_TOKEN_TEST, ipv4Address.getPublicAddress());
  }

  @Test
  void testRetrieveDevice_InvalidToken() {
    mockRequestWithToken(INVALID_BEARER_TOKEN);
    var tokenDevice = assertDoesNotThrow(() -> tokenService.retrieveDevice());
    assertNull(tokenDevice);
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testRetrieveClientId_InvalidToken(CapturedOutput output) {
    mockRequestWithToken(INVALID_BEARER_TOKEN);

    var clientId = assertDoesNotThrow(() -> tokenService.retrieveClientId());
    assertNull(clientId);
    assertTrue(output.getAll().contains("Invalid token in header provided"));
  }

  @Test
  void testRetrieveClientId_WithoutHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    RequestAttributes requestAttributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(requestAttributes);

    var clientId = assertDoesNotThrow(() -> tokenService.retrieveClientId());
    assertNull(clientId);
  }

  @Test
  void testRetrieveClientId_WithoutBearerInHeader() {
    mockRequestWithToken("invalid");
    var clientId = assertDoesNotThrow(() -> tokenService.retrieveClientId());
    assertNull(clientId);
  }
}
