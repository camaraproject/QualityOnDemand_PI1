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

import static com.camara.qod.config.QodConfig.IPV4_PATTERN;
import static com.camara.qod.config.QodConfig.IPV4_WITH_NETWORK_SEGMENT_REGEX;

import com.camara.qod.api.model.BaseSessionInfoWebhook;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.DeviceIpv4Addr;
import com.camara.qod.config.QodConfig;
import com.camara.qod.exception.ErrorCode;
import com.camara.qod.exception.QodApiException;
import jakarta.validation.constraints.NotNull;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Matcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ValidationService {

  private final QodConfig qodConfig;

  /**
   * Validates a {@link CreateSession}.
   *
   * @param createSession the {@link CreateSession}
   */
  public void validate(@NotNull CreateSession createSession) {
    validateDuration(createSession.getDuration());
    validateAppServerNetwork(createSession.getApplicationServer().getIpv4Address());
    validateDeviceNetwork(createSession.getDevice().getIpv4Address());

    BaseSessionInfoWebhook webhook = createSession.getWebhook();
    if (Objects.nonNull(webhook)) {
      validateNotificationUrl(webhook.getNotificationUrl());
    }
  }

  private void validateNotificationUrl(URI notificationUrl) {
    try {
      new URL(notificationUrl.toString()).toURI();
    } catch (URISyntaxException | MalformedURLException e) {
      throw new QodApiException(HttpStatus.BAD_REQUEST, "Validation failed for parameter 'notificationUrl' - Invalid URL-Syntax.",
          ErrorCode.VALIDATION_FAILED);
    }
  }

  private void validateDuration(Integer duration) {
    if (Objects.isNull(duration)) {
      throw new QodApiException(HttpStatus.BAD_REQUEST, "Validation failed for parameter 'duration'", ErrorCode.INVALID_INPUT);
    }
  }

  private void validateDeviceNetwork(DeviceIpv4Addr deviceIpv4) {
    validateExistingParameter(deviceIpv4, "device.ipv4Address");
    String publicIpv4FieldName = "device.ipv4Address.publicAddress";
    validateExistingParameter(deviceIpv4.getPublicAddress(), publicIpv4FieldName);
    validateIpv4Address(deviceIpv4.getPublicAddress(), publicIpv4FieldName);
  }

  private void validateAppServerNetwork(String appServerIpv4) {
    validateExistingParameter(appServerIpv4, "applicationServer.ipv4Address");
    validateIpv4Address(appServerIpv4, "applicationServer.ipv4Address");
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
}
