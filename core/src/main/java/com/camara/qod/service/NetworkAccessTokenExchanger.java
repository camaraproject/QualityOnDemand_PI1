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

import com.camara.qod.config.NetworkConfig;
import com.camara.qod.model.AccessTokenResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
@Slf4j
public class NetworkAccessTokenExchanger {

  private final RestTemplate restTemplate;
  private final NetworkConfig networkConfig;

  private static MultiValueMap<String, String> createForm() {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "client_credentials");
    return params;
  }

  /**
   * Gets access token using oauth2 client credentials flow.
   */
  public String exchange() {
    HttpHeaders headers = createHeaders();
    MultiValueMap<String, String> params = createForm();
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
    ResponseEntity<AccessTokenResponse> response = restTemplate.postForEntity(networkConfig.getTokenEndpoint(), request,
        AccessTokenResponse.class);
    AccessTokenResponse accessTokenResponse = response.getBody();
    if (accessTokenResponse != null) {
      return accessTokenResponse.getAccessToken();
    } else {
      log.warn("No access-token provided from the token-provider.");
      return "";
    }
  }

  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setBasicAuth(networkConfig.getClientId(), networkConfig.getClientSecret());
    return headers;
  }
}
