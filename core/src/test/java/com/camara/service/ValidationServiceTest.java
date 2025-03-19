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

import static com.camara.util.SessionsTestData.TEST_SINK;
import static com.camara.util.SessionsTestData.createRetrieveSessionsRequest;
import static com.camara.util.SessionsTestData.createTestSession;
import static com.camara.util.SessionsTestData.createValidTestSession;
import static com.camara.util.TokenTestData.createDevice;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.camara.config.QodConfig;
import com.camara.exception.ErrorCode;
import com.camara.exception.QodApiException;
import com.camara.mapping.DeviceMapper;
import com.camara.model.SupportedQosProfiles;
import com.camara.quality_on_demand.api.model.CreateSession;
import com.camara.quality_on_demand.api.model.DeviceIpv4Addr;
import com.camara.quality_on_demand.api.model.PlainCredential;
import com.camara.quality_on_demand.api.model.RetrieveSessionsInput;
import com.camara.quality_on_demand.api.model.SinkCredential.CredentialTypeEnum;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;

class ValidationServiceTest {

  AutoCloseable closeable;

  @InjectMocks
  private ValidationService validationService;

  @Mock
  private QodConfig qodConfig;

  @Mock
  private TokenService tokenService;

  @Spy
  private DeviceMapper deviceMapper = Mappers.getMapper(DeviceMapper.class);


  private CreateSession testSessionRequest;
  private RetrieveSessionsInput testRetrieveSessionRequest;

  @SneakyThrows
  @BeforeEach
  void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
    when(qodConfig.isQosAllowMultipleDeviceAddr()).thenReturn(true);
    when(tokenService.retrieveDevice()).thenReturn(null);
    testSessionRequest = createTestSession(SupportedQosProfiles.QOS_L);
    testRetrieveSessionRequest = createRetrieveSessionsRequest();
  }

  @SneakyThrows
  @AfterEach
  void tearDown() {
    closeable.close();
  }

  @Test
  void testValidateRetrieveSessionsRequest_Ok_200() {
    assertDoesNotThrow(() -> validationService.validate(testRetrieveSessionRequest));
  }

  @Test
  void testValidateCreateSession_Ok_200() {
    assertDoesNotThrow(() -> validationService.validate(testSessionRequest));
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testValidateCreateSession_Ipv4InTokenAndRequest_Ok(CapturedOutput output) {
    var deviceFromToken = createDevice();
    when(tokenService.retrieveDevice()).thenReturn(deviceFromToken);
    assertDoesNotThrow(() -> validationService.validate(testSessionRequest));
    assertTrue(output.getAll().contains(
        "Device identifier from the request matches the identifier from the authorization regarding <device.ipv4Address.publicAddress>"));
  }

  @Test
  void testValidateCreateSession_Ipv4OnlyInToken_Ok() {
    var deviceFromToken = createDevice();
    when(tokenService.retrieveDevice()).thenReturn(deviceFromToken);
    assertDoesNotThrow(() -> validationService.validate(testSessionRequest.device(null)));
  }

  @Test
  void testValidateCreateSession_BadRequest_InvalidDeviceIpv4Addr_400() {
    testSessionRequest.getDevice().setIpv4Address(new DeviceIpv4Addr().publicAddress("invalid"));
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> validationService.validate(testSessionRequest));
    assertEquals(HttpStatus.BAD_REQUEST, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.VALIDATION_FAILED.name(), qodApiException.getErrorCode());
    assertEquals("Network specification for device.ipv4Address.publicAddress not valid: <invalid>", qodApiException.getMessage());
  }

  @Test
  void testValidateCreateSession_BadRequest_MissingDeviceIpv4Addr_400() {
    testSessionRequest.getDevice().setIpv4Address(null);
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> validationService.validate(testSessionRequest));
    assertEquals(HttpStatus.BAD_REQUEST, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.PARAMETER_MISSING.name(), qodApiException.getErrorCode());
    assertEquals("Validation failed for parameter 'device.ipv4Address'", qodApiException.getMessage());
  }

  @Test
  void testValidateCreateSession_BadRequest_UnsupportedDeviceIdentifiers_400() {
    testSessionRequest.getDevice().setPhoneNumber("+49123412456");
    testSessionRequest.getDevice().setNetworkAccessIdentifier("test@example");
    testSessionRequest.getDevice().setIpv6Address("2001:0000:130F:0000:0000:09C0:876A:130B");
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> validationService.validate(testSessionRequest));
    assertEquals(HttpStatus.BAD_REQUEST, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.UNSUPPORTED_DEVICE_IDENTIFIERS.name(), qodApiException.getErrorCode());
    assertEquals("Only 'ipv4Address' is currently supported.", qodApiException.getMessage());
  }

  @Test
  void testValidateCreateSession_BadRequest_MissingDeviceIpv4PublicAddress_400() {
    testSessionRequest.getDevice().setIpv4Address(new DeviceIpv4Addr().publicAddress(null));
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> validationService.validate(testSessionRequest));
    assertEquals(HttpStatus.BAD_REQUEST, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.PARAMETER_MISSING.name(), qodApiException.getErrorCode());
    assertEquals("Validation failed for parameter 'device.ipv4Address.publicAddress'", qodApiException.getMessage());
  }

  @Test
  void testValidateCreateSession_BadRequest_MissingAsIdIpv4Addr_400() {
    testSessionRequest.getApplicationServer().setIpv4Address(null);
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> validationService.validate(testSessionRequest));
    assertEquals(HttpStatus.BAD_REQUEST, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.PARAMETER_MISSING.name(), qodApiException.getErrorCode());
    assertEquals("Validation failed for parameter 'applicationServer.ipv4Address'", qodApiException.getMessage());
  }

  @Test
  void testValidateCreateSession_BadRequest_NetworkSegmentNotAllowed_400() {
    when(qodConfig.isQosAllowMultipleDeviceAddr()).thenReturn(false);
    testSessionRequest.getDevice().setIpv4Address(new DeviceIpv4Addr().publicAddress("10.0.0.0/8"));
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> validationService.validate(testSessionRequest));
    assertEquals(HttpStatus.BAD_REQUEST, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.NOT_ALLOWED.name(), qodApiException.getErrorCode());
    assertEquals("A network segment for Device IPv4 is not allowed in the current configuration: "
        + "10.0.0.0/8 is not allowed, but 10.0.0.0 is allowed.", qodApiException.getMessage());
  }

  @Test
  @SneakyThrows
  void testValidateCreateSession_BadRequest_InvalidSink_400() {
    CreateSession session = createValidTestSession();
    session.sink("invalid");
    QodApiException exception = assertThrows(QodApiException.class, () -> validationService.validate(session));
    assertEquals("Validation failed for parameter 'sink' - Invalid URL-Syntax.", exception.getMessage());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  @SneakyThrows
  void testValidateCreateSession_BadRequest_InvalidSinkCredentialType_400() {
    CreateSession session = createValidTestSession();
    session.sink(TEST_SINK);
    PlainCredential plainCredential = new PlainCredential();
    plainCredential.setCredentialType(CredentialTypeEnum.PLAIN);
    plainCredential.setIdentifier("id");
    plainCredential.setSecret("secret");
    session.setSinkCredential(plainCredential);
    QodApiException exception = assertThrows(QodApiException.class, () -> validationService.validate(session));
    assertEquals("Only Access token is supported", exception.getMessage());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void testValidateCreateSession_Ipv4InTokenMismatchWithRequestBody_Forbidden_403() {
    var deviceFromToken = createDevice();
    deviceFromToken.getIpv4Address().setPublicAddress("123.0.0.1");
    when(tokenService.retrieveDevice()).thenReturn(deviceFromToken);
    QodApiException qodApiException = assertThrows(QodApiException.class, () -> validationService.validate(testSessionRequest));
    assertEquals(HttpStatus.FORBIDDEN, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.INVALID_TOKEN_CONTEXT.name(), qodApiException.getErrorCode());
    assertEquals("<172.24.11.4> is not consistent with access token.", qodApiException.getMessage());
  }

  @Test
  void testValidateCreateSession_UnidentifiableDevice_422() {
    when(tokenService.retrieveDevice()).thenReturn(null);
    QodApiException qodApiException = assertThrows(QodApiException.class,
        () -> validationService.validate(testSessionRequest.device(null)));
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, qodApiException.getHttpStatus());
    assertEquals(ErrorCode.UNIDENTIFIABLE_DEVICE.name(), qodApiException.getErrorCode());
    assertEquals("The device cannot be identified.", qodApiException.getMessage());
  }
}
