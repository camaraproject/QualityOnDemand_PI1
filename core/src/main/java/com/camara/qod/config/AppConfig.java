/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 Contributors | Deutsche Telekom AG to CAMARA a Series of LF Projects, LLC
 * The contributor of this file confirms his sign-off for the
 * Developer Certificate of Origin (http://developercertificate.org).
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

package com.camara.qod.config;

import com.camara.scef.api.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * This class contains general configurations for the application.
 */
@Configuration
public class AppConfig {

  private static final String BASE_URL_TEMPLATE = "%s/3gpp-as-session-with-qos/v1";

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  /**
   * Creates a {@link ApiClient}.
   *
   * @return the created client.
   */
  @Bean
  public ApiClient apiClient(ScefConfig scefConfig) {
    ApiClient apiClient =
        new ApiClient().setBasePath(String.format(BASE_URL_TEMPLATE, scefConfig.getApiRoot()));
    if (scefConfig.getAuthMethod().equals("basic")) {
      apiClient.setUsername(scefConfig.getUserName());
      apiClient.setPassword(scefConfig.getPassword());
    } else {
      apiClient.setAccessToken(scefConfig.getToken());
    }
    apiClient.setDebugging(scefConfig.getScefDebug());
    return apiClient;
  }
}
