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

package com.camara.qod.service;

import static com.camara.qod.util.SessionsTestData.createTestSession;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.DeviceIpv4Addr;
import com.camara.qod.config.QodConfig;
import com.camara.qod.exception.ErrorCode;
import com.camara.qod.exception.QodApiException;
import com.camara.qod.model.SupportedQosProfiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

class ValidationServiceTest {

  AutoCloseable closeable;

  @InjectMocks
  private ValidationService validationService;

  @Mock
  private QodConfig qodConfig;

  private CreateSession testSession;

  @BeforeEach
  void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
    when(qodConfig.isQosAllowMultipleDeviceAddr()).thenReturn(true);
    testSession = createTestSession(SupportedQosProfiles.QOS_L);
  }

  @Test
  void testValidateCreateSession_Ok_200() {
    assertDoesNotThrow(() -> validationService.validate(testSession));
  }

  @Test
  void testValidateCreateSession_BadRequest_MissingDeviceIpv4Addr_400() {
    testSession.getDevice().setIpv4Address(null);
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> validationService.validate(testSession));
    assertEquals(HttpStatus.BAD_REQUEST, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.PARAMETER_MISSING.name(), qodApiException.getErrorCode());
    assertEquals("Validation failed for parameter 'device.ipv4Address'", qodApiException.getMessage());
  }

  @Test
  void testValidateCreateSession_BadRequest_MissingDeviceIpv4PublicAddress_400() {
    testSession.getDevice().setIpv4Address(new DeviceIpv4Addr().publicAddress(null));
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> validationService.validate(testSession));
    assertEquals(HttpStatus.BAD_REQUEST, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.PARAMETER_MISSING.name(), qodApiException.getErrorCode());
    assertEquals("Validation failed for parameter 'device.ipv4Address.publicAddress'", qodApiException.getMessage());
  }

  @Test
  void testValidateCreateSession_BadRequest_MissingAsIdIpv4Addr_400() {
    testSession.getApplicationServer().setIpv4Address(null);
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> validationService.validate(testSession));
    assertEquals(HttpStatus.BAD_REQUEST, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.PARAMETER_MISSING.name(), qodApiException.getErrorCode());
    assertEquals("Validation failed for parameter 'applicationServer.ipv4Address'", qodApiException.getMessage());
  }

  @Test
  void testValidateCreateSession_BadRequest_NetworkSegmentNotAllowed_400() {
    when(qodConfig.isQosAllowMultipleDeviceAddr()).thenReturn(false);
    testSession.getDevice().setIpv4Address(new DeviceIpv4Addr().publicAddress("10.0.0.0/8"));
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> validationService.validate(testSession));
    assertEquals(HttpStatus.BAD_REQUEST, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.NOT_ALLOWED.name(), qodApiException.getErrorCode());
    assertEquals("A network segment for Device IPv4 is not allowed in the current configuration: "
        + "10.0.0.0/8 is not allowed, but 10.0.0.0 is allowed.", qodApiException.getMessage());
  }
}
