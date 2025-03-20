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

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.camara.config.QodConfig;
import com.camara.model.Device;
import com.camara.model.Device.DeviceIpv4Addr;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

  private static final String CLIENT_ID_ANONYMOUS = "anonymous";
  private static final String CLIENT_ID_CLAIM = "clientId";
  private static final String DEVICE_IPV4_PUBLIC_ADDRESS_CLAIM = "ipv4Address_public";
  private static final String DEVICE_IPV4_PRIVATE_ADDRESS_CLAIM = "ipv4Address_private";

  private final QodConfig qodConfig;
  private DecodedJWT decodedToken;

  /**
   * Retrieves device identifiers from the token.
   *
   * @return a filled in {@link Device} based on device identifiers in the token
   */
  public Device retrieveDevice() {
    if (!isValidToken()) {
      return null;
    }
    String publicIpv4 = retrieveIpv4PublicAddress();
    String privateIpv4 = retrieveIpv4PrivateAddress();
    if (StringUtils.isAllEmpty(publicIpv4, privateIpv4)) {
      log.info("No device contained in access-token");
      return null;
    } else {
      var deviceIpv4Addr = DeviceIpv4Addr.builder()
          .publicAddress(publicIpv4)
          .privateAddress(privateIpv4)
          .build();
      return Device.builder()
          .ipv4Address(deviceIpv4Addr)
          .build();
    }
  }

  /**
   * Retrieves the clientId from the token.
   *
   * @return A String that represents the required value for the claim. If it is not present, then return null.
   */
  public String retrieveClientId() {
    String clientId = null;
    if (isValidToken()) {
      clientId = retrieveValueFromClaim(CLIENT_ID_CLAIM);
    }

    if (StringUtils.isBlank(clientId)
        && qodConfig.isAllowAnonymousClients()) {
      clientId = CLIENT_ID_ANONYMOUS;
    }
    return clientId;
  }

  /**
   * Retrieves the IPv4 public address from the token.
   *
   * @return A String that represents the required value for the claim. If it is not present, then return null.
   */
  private String retrieveIpv4PublicAddress() {
    return retrieveValueFromClaim(DEVICE_IPV4_PUBLIC_ADDRESS_CLAIM);
  }

  /**
   * Retrieves the IPv4 private address from the token.
   *
   * @return A String that represents the required value for the claim. If it is not present, then return null.
   */
  private String retrieveIpv4PrivateAddress() {
    return retrieveValueFromClaim(DEVICE_IPV4_PRIVATE_ADDRESS_CLAIM);
  }

  private boolean isValidToken() {
    log.debug("Retrieving authorization header");
    var authorizationHeader = retrieveAuthorizationHeader();
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      String token = authorizationHeader.substring(7);
      decodedToken = decodeJwt(token);
      return decodedToken != null;
    } else {
      return false;
    }
  }

  private DecodedJWT decodeJwt(String token) {
    try {
      return JWT.decode(token);
    } catch (JWTDecodeException ex) {
      log.debug("Invalid token provided: {}", ex.getMessage(), ex);
      log.error("Invalid token in header provided. Values cannot be extracted.");
      return null;
    }
  }

  private String retrieveValueFromClaim(String claimName) {
    log.debug("Retrieving value from claim {} from token", claimName);
    Claim claim = decodedToken.getClaim(claimName);
    if (claim.isMissing()) {
      log.debug("The claim {} is not present in the provided token", claimName);
    }
    return claim.asString();
  }

  private static String retrieveAuthorizationHeader() {
    RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
    HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
    return request.getHeader(AUTHORIZATION);
  }
}
