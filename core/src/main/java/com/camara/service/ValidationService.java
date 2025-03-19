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

import static com.camara.config.QodConfig.IPV4_PATTERN;
import static com.camara.config.QodConfig.IPV4_WITH_NETWORK_SEGMENT_REGEX;

import com.camara.config.QodConfig;
import com.camara.entity.QosSession;
import com.camara.exception.ErrorCode;
import com.camara.exception.QodApiException;
import com.camara.mapping.DeviceMapper;
import com.camara.model.Device.DeviceIpv4Addr;
import com.camara.model.DeviceIdentifierTypes;
import com.camara.qos_profiles.api.model.QosProfile;
import com.camara.qos_profiles.api.model.QosProfileStatusEnum;
import com.camara.quality_on_demand.api.model.CreateSession;
import com.camara.quality_on_demand.api.model.Device;
import com.camara.quality_on_demand.api.model.QosStatus;
import com.camara.quality_on_demand.api.model.RetrieveSessionsInput;
import com.camara.quality_on_demand.api.model.SinkCredential;
import com.camara.quality_on_demand.api.model.SinkCredential.CredentialTypeEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ValidationService {

  private final DeviceMapper deviceMapper;
  private final QodConfig qodConfig;
  private final QosProfileService qosProfileService;
  private final TokenService tokenService;


  /**
   * Validates if the requested duration in fits in the {@link QosProfile}.
   *
   * @param duration   the duration
   * @param qosProfile {@link QosProfile}
   */
  public void validateDurationWithQosProfile(Integer duration, QosProfile qosProfile) {
    validateQosProfileAvailability(qosProfile.getStatus());
    checkDurationInProfileRange(duration, qosProfile);
  }

  /**
   * Checks if the status of the QoS-Profile is in state "ACTIVE".
   *
   * @param status {@link QosProfileStatusEnum}
   */
  public void validateQosProfileAvailability(@NotNull @Valid QosProfileStatusEnum status) {
    if (status != QosProfileStatusEnum.ACTIVE) {
      throw new QodApiException(HttpStatus.BAD_REQUEST, "Requested QoS profile currently unavailable");
    }
  }

  private void checkDurationInProfileRange(Integer duration, QosProfile qosProfile) {
    long minDurationSeconds = qosProfileService.retrieveDurationInSeconds(qosProfile.getMinDuration());
    long maxDurationSeconds = qosProfileService.retrieveDurationInSeconds(qosProfile.getMaxDuration());
    if (duration < minDurationSeconds || maxDurationSeconds < duration) {
      throw new QodApiException(HttpStatus.BAD_REQUEST,
          "The requested duration is out of the allowed range for the specific QoS profile: " + qosProfile.getName(),
          ErrorCode.getQualityOnDemandErrorCode(ErrorCode.DURATION_OUT_OF_RANGE.name()));
    }
  }

  /**
   * Declines all unsupported device identifiers, except 'ipv4Address'.
   *
   * @param phoneNumber             the phone number
   * @param networkAccessIdentifier the networkAccessIdentifier
   * @param ipv6Address             the ipv6Address
   */
  private void declineUnsupportedDeviceIds(String phoneNumber, String networkAccessIdentifier, String ipv6Address) {
    if (networkAccessIdentifier != null || phoneNumber != null || ipv6Address != null) {
      throw new QodApiException(HttpStatus.BAD_REQUEST, "Only 'ipv4Address' is currently supported.",
          ErrorCode.UNSUPPORTED_DEVICE_IDENTIFIERS);
    }
  }

  /**
   * Checks if a {@link QosSession} is allowed to be extended.
   *
   * @param qosSession {@link QosSession}
   */
  public void isSessionExtendable(QosSession qosSession) {
    if (qosSession.getQosStatus() != QosStatus.AVAILABLE) {
      throw new QodApiException(HttpStatus.CONFLICT,
          "Extending the session duration is not allowed in the current state " + qosSession.getQosStatus() + "."
              + " The session must be in the AVAILABLE state.",
          ErrorCode.getQualityOnDemandErrorCode(ErrorCode.SESSION_EXTENSION_NOT_ALLOWED.name()));
    }

    // ExpiredSessionMonitor -> check session handling in case of session will be expired next
    if (qosSession.isScheduledForDeletion()) {
      throw new QodApiException(HttpStatus.NOT_FOUND,
          "The Quality of Service (QoS) session has reached its expiration, and the deletion process is running.");
    }
  }

  /**
   * Validates a {@link RetrieveSessionsInput} request.
   *
   * @param request {@link RetrieveSessionsInput}
   */
  public void validate(RetrieveSessionsInput request) {
    com.camara.model.Device requestedDevice = deviceMapper.toCommonDevice(request.getDevice());
    validateDeviceAndToken(
        requestedDevice,
        tokenService.retrieveDevice(),
        request::setDevice
    );
  }

  /**
   * Validates a {@link CreateSession} request.
   *
   * @param request {@link CreateSession}
   */
  public void validate(CreateSession request) {
    com.camara.model.Device requestedDevice = deviceMapper.toCommonDevice(request.getDevice());
    validateDeviceAndToken(
        requestedDevice,
        tokenService.retrieveDevice(),
        request::setDevice
    );

    validateAppServerNetwork(request.getApplicationServer().getIpv4Address());
    validateSink(request.getSink());
    validateSinkCredential(request.getSinkCredential());
  }

  private void validateDeviceAndToken(com.camara.model.Device requestDevice, com.camara.model.Device tokenDevice,
      Consumer<Device> request) {
    if (requestDevice != null && tokenDevice != null) {
      checkOnDeviceIdentifierMismatch(requestDevice, tokenDevice);
      validateDevice(requestDevice);
      validateDevice(tokenDevice);
    } else if (requestDevice != null) {
      validateDevice(requestDevice);
    } else if (tokenDevice != null) {
      validateDevice(tokenDevice);
      var mappedDevice = deviceMapper.toSessionsDevice(tokenDevice);
      request.accept(mappedDevice);
    } else {
      throw new QodApiException(HttpStatus.UNPROCESSABLE_ENTITY, "The device cannot be identified.", ErrorCode.UNIDENTIFIABLE_DEVICE);
    }
  }

  private void validateDevice(com.camara.model.Device requestDevice) {
    declineUnsupportedDeviceIds(requestDevice.getPhoneNumber(), requestDevice.getNetworkAccessIdentifier(), requestDevice.getIpv6Address());
    validateIpv4Address(requestDevice.getIpv4Address());
  }

  private void validateIpv4Address(DeviceIpv4Addr deviceIpv4) {
    validateExistingParameter(deviceIpv4, "device.ipv4Address");
    String publicIpv4FieldName = "device.ipv4Address.publicAddress";
    validateExistingParameter(deviceIpv4.getPublicAddress(), publicIpv4FieldName);
    validateIpv4Address(deviceIpv4.getPublicAddress(), publicIpv4FieldName);
  }

  /**
   * Validates an IPv4 address against a specified regular expression pattern.
   *
   * @param ipAddress     The IPv4 address to validate.
   * @param parameterName The name of the parameter associated with the IPv4 address.
   * @throws QodApiException if the provided IPv4 address is not valid, according to the pattern.
   */
  private void validateIpv4Address(String ipAddress, String parameterName) {
    if (ipAddress != null) {
      // if multiple device.Ipv4Addr are not allowed and specified device.Ipv4Addr is a network segment, return error
      validateNetworkSegment(ipAddress);
      Matcher matcher = IPV4_PATTERN.matcher(ipAddress);
      if (!matcher.matches()) {
        throw new QodApiException(HttpStatus.BAD_REQUEST, "Network specification for " + parameterName + " not valid: <" + ipAddress + ">",
            ErrorCode.VALIDATION_FAILED);
      }
    }
  }

  private void validateSink(String sink) {
    if (Objects.nonNull(sink)) {
      try {
        URL url = new URI(sink).toURL();
        log.debug("Successfully validated sink-URL: {}", url);
      } catch (Exception e) {
        log.debug(e.getMessage());
        throw new QodApiException(HttpStatus.BAD_REQUEST, "Validation failed for parameter 'sink' - Invalid URL-Syntax.",
            ErrorCode.VALIDATION_FAILED);
      }
    }
  }

  private void validateSinkCredential(SinkCredential sinkCredential) {
    if (Objects.nonNull(sinkCredential) && sinkCredential.getCredentialType() != CredentialTypeEnum.ACCESSTOKEN) {
      log.debug("rejected - only Access token as sinkCredential is supported");
      throw new QodApiException(HttpStatus.BAD_REQUEST, "Only Access token is supported", ErrorCode.INVALID_CREDENTIAL);
    }
  }

  private void validateAppServerNetwork(String appServerIpv4) {
    validateExistingParameter(appServerIpv4, "applicationServer.ipv4Address");
    validateIpv4Address(appServerIpv4, "applicationServer.ipv4Address");
  }

  private void validateNetworkSegment(String ipAddress) {
    if (!qodConfig.isQosAllowMultipleDeviceAddr() && ipAddress.matches(IPV4_WITH_NETWORK_SEGMENT_REGEX)) {
      throw new QodApiException(HttpStatus.BAD_REQUEST,
          "A network segment for Device IPv4 is not allowed in the current configuration: " + ipAddress + " is not allowed, but "
              + ipAddress.substring(0, ipAddress.indexOf("/")) + " is allowed.", ErrorCode.NOT_ALLOWED);
    }
  }

  private void validateExistingParameter(Object parameter, String parameterName) {
    if (parameter == null) {
      throw new QodApiException(HttpStatus.BAD_REQUEST, "Validation failed for parameter '" + parameterName + "'",
          ErrorCode.PARAMETER_MISSING);
    }
  }

  private void checkOnDeviceIdentifierMismatch(com.camara.model.Device requestDevice, com.camara.model.Device tokenDevice) {

    checkDeviceIdentifierMatch(requestDevice.getPhoneNumber(), tokenDevice.getPhoneNumber(), DeviceIdentifierTypes.PHONE_NUMBER.getValue());

    var requestDeviceIpv4Address = requestDevice.getIpv4Address();
    var tokenDeviceIpv4Address = tokenDevice.getIpv4Address();

    if (requestDeviceIpv4Address != null && tokenDeviceIpv4Address != null) {
      checkDeviceIdentifierMatch(requestDeviceIpv4Address.getPublicAddress(), tokenDeviceIpv4Address.getPublicAddress(),
          DeviceIdentifierTypes.IPV4_PUBLIC_ADDRESS.getValue());
      checkDeviceIdentifierMatch(requestDeviceIpv4Address.getPrivateAddress(), tokenDeviceIpv4Address.getPrivateAddress(),
          DeviceIdentifierTypes.IPV4_PRIVATE_ADDRESS.getValue());
    }
  }

  private void checkDeviceIdentifierMatch(String requestValue, String tokenValue, String identifierType) {
    if (StringUtils.isAnyEmpty(requestValue, tokenValue)) {
      log.debug("No match-check for {} as one of the values is empty: requested <{}>, from token <{}>",
          identifierType, requestValue, tokenValue);
    } else if (StringUtils.equals(requestValue, tokenValue)) {
      log.info("Device identifier from the request matches the identifier from the authorization regarding <{}>", identifierType);
    } else {
      throw new QodApiException(HttpStatus.FORBIDDEN,
          "<" + requestValue + "> is not consistent with access token.",
          ErrorCode.INVALID_TOKEN_CONTEXT);
    }
  }
}
