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

package com.camara.security;

import com.camara.config.QodConfig;
import com.camara.quality_on_demand.api.model.ErrorInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import lombok.Generated;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@ConditionalOnProperty(prefix = "device-location", name = "notifications.ip-filter.enabled", havingValue = "true")
@RequiredArgsConstructor
@Component
@Slf4j
public class Ipv4AllowListFilter extends OncePerRequestFilter {

  private final Set<String> allowedIpv4Addresses = new HashSet<>();

  private final QodConfig qodConfig;

  @Override
  @Generated
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !path.contains("/notifications");
  }

  @Override
  protected void initFilterBean() {
    allowedIpv4Addresses.addAll(qodConfig.getAllowedIpv4Addresses());
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    String ipAddress = request.getRemoteAddr();

    if (allowedIpv4Addresses.contains(ipAddress)) {
      chain.doFilter(request, response);
    } else {
      log.debug("Requested IPv4 {} is not allowed to perform this action.", ipAddress);
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      ErrorInfo errorResponse = new ErrorInfo()
          .message("Access denied");
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write(convertObjectToJson(errorResponse));
    }
  }

  @Generated
  private String convertObjectToJson(Object object) throws JsonProcessingException {
    if (object == null) {
      return null;
    }
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(object);
  }
}
