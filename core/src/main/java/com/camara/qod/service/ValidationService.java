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

import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.CreateSessionWebhook;
import com.camara.qod.api.model.DeviceIpv4Addr;
import com.camara.qod.config.QodConfig;
import com.camara.qod.exception.ErrorCode;
import com.camara.qod.exception.QodApiException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import jakarta.validation.constraints.NotNull;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
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

    CreateSessionWebhook webhook = createSession.getWebhook();
    if (Objects.nonNull(webhook)) {
      validateNotificationUrl(webhook.getNotificationUrl());
    }
  }

  private void validateNotificationUrl(URI notificationUrl) {
    try {
      new URL(notificationUrl.toString()).toURI();
    } catch (URISyntaxException | MalformedURLException e) {
      throw new QodApiException(
          HttpStatus.BAD_REQUEST,
          "Validation failed for parameter 'notificationUrl' - Invalid URL-Syntax.",
          ErrorCode.VALIDATION_FAILED);
    }
  }

  private void validateDuration(Integer duration) {
    if (Objects.isNull(duration)) {
      throw new QodApiException(HttpStatus.BAD_REQUEST,
          "Validation failed for parameter 'duration'",
          ErrorCode.INVALID_INPUT);
    }
  }

  private void validateDeviceNetwork(DeviceIpv4Addr deviceIpv4) {
    validateExistingParameter(deviceIpv4, "device.ipv4Address");
    validateExistingParameter(deviceIpv4.getPublicAddress(), "device.ipv4Address.publicAddress");
    validateIpAddress(deviceIpv4.getPublicAddress(), "device.ipv4Address.publicAddress");
    validateNetworkSegment(deviceIpv4.getPublicAddress());
  }

  private void validateNetworkSegment(String deviceIpv4) {
    // if multiple device.Ipv4Addr are not allowed and specified device.Ipv4Addr is a network segment, return error
    if (!qodConfig.isQosAllowMultipleDeviceAddr() && deviceIpv4.matches(QodConfig.NETWORK_SEGMENT_REGEX)) {
      throw new QodApiException(HttpStatus.BAD_REQUEST,
          "A network segment for Device IPv4 is not allowed in the current configuration: " + deviceIpv4
              + " is not allowed, but " + deviceIpv4.substring(0, deviceIpv4.indexOf("/"))
              + " is allowed.", ErrorCode.NOT_ALLOWED);
    }
  }

  private void validateAppServerNetwork(String appServerIpv4) {
    validateExistingParameter(appServerIpv4, "applicationServer.ipv4Address");
    validateIpAddress(appServerIpv4, "device.ipv4Address.publicAddress");
  }

  /**
   * Checks if network is defined with the start address, e.g., 2001:db8:85a3:8d3:1319:8a2e:370:7344/128 and not
   * 2001:db8:85a3:8d3:1319:8a2e:370:7344/120.
   */
  private void validateIpAddress(String network, String parameterName) {
    IPAddress current = new IPAddressString(network).getAddress();
    IPAddress rewritten = current.toPrefixBlock();
    if (current != rewritten
        || network.split("\\.").length != 4) {
      throw new QodApiException(
          HttpStatus.BAD_REQUEST, "Network specification for " + parameterName + " not valid " + network, ErrorCode.VALIDATION_FAILED);
    }
  }

  private void validateExistingParameter(Object parameter, String parameterName) {
    if (parameter == null) {
      throw new QodApiException(HttpStatus.BAD_REQUEST, "Validation failed for parameter '" + parameterName + "'",
          ErrorCode.PARAMETER_MISSING);
    }
  }
}
