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

package com.camara.qod.security;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class EnhancedTokenValidationFilter extends OncePerRequestFilter {

  private static final String REQUEST_PATH_CLAIM = "requestPath";
  private static final String OPERATION_CLAIM = "operation";
  private static final Set<String> PROTECTED_SERVLET_PATH_SUFFIXES = new HashSet<>(Arrays.asList("/sessions", "/notifications"));
  private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

  @Value("${enhanced-token-validation.enabled}")
  private Boolean isEnhancedTokenValidationEnabled;

  private static String getEndpointName(String servletPath) {
    return servletPath.substring(servletPath.lastIndexOf('/'));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    log.debug("Validate request details specific token claims");
    try {
      Map<String, Claim> claims = extractClaimsFromToken(request.getHeader(AUTHORIZATION));
      validateRequestSpecificTokenClaims(claims, request);
    } catch (TokenProcessingException e) {
      log.error(e.getMessage());
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    log.info("Request details specific claims validated successfully");
    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String servletPath = request.getServletPath();
    return !isEnhancedTokenValidationEnabled || !(PROTECTED_SERVLET_PATH_SUFFIXES.stream().anyMatch(servletPath::endsWith)
        || servletPath.substring(servletPath.lastIndexOf("/") + 1).matches(UUID_REGEX));
  }

  private void validateRequestSpecificTokenClaims(Map<String, Claim> claims, HttpServletRequest request) {
    String requestPathClaim = Optional.of(claims.get(REQUEST_PATH_CLAIM)).map(Claim::asString)
        .orElseThrow(() -> new TokenProcessingException("requestPath claim is not present"));
    String operationClaim = Optional.of(claims.get(OPERATION_CLAIM)).map(Claim::asString)
        .orElseThrow(() -> new TokenProcessingException("operation claim is not present"));
    String endpointNameFromServlet = getEndpointName(request.getServletPath());
    String endpointNameFromToken = getEndpointName(requestPathClaim);
    if (!Objects.equals(endpointNameFromToken, endpointNameFromServlet)) {
      throw new TokenProcessingException(
          String.format("endpoint name from token (%s) is not equal actual endpoint name from request (%s)", endpointNameFromToken,
              endpointNameFromServlet));
    }
    if (!Objects.equals(request.getMethod(), operationClaim)) {
      throw new TokenProcessingException(
          String.format("operation claim (%s) is not equal actual request method from request (%s)", operationClaim, request.getMethod()));
    }
  }

  private Map<String, Claim> extractClaimsFromToken(String authorizationHeader) {
    log.debug("Extracting claims from token");
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      String token = authorizationHeader.substring(7);
      DecodedJWT decodedJwt = JWT.decode(token);
      return decodedJwt.getClaims();
    }
    throw new TokenProcessingException("Token is absent or has incorrect format");
  }
}
