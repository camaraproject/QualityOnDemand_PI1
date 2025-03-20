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

package com.camara.config;

import com.camara.network.api.ApiClient;
import java.net.InetSocketAddress;
import java.net.Proxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * This class contains general configurations for the application.
 */
@Configuration
@ConditionalOnProperty(prefix = "network", name = "server.mock.enabled", havingValue = "false", matchIfMissing = true)
public class AppConfig {

  private static final String BASE_URL_TEMPLATE = "%s/3gpp-as-session-with-qos/v1";

  @Value("${app.proxy.enabled}")
  private boolean isProxyEnabled;

  @Value("${app.proxy.host}")
  private String proxyHost;

  @Value("${app.proxy.port}")
  private int proxyPort;

  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    if (isProxyEnabled) {
      requestFactory.setProxy(new Proxy(Proxy.Type.HTTP,
          new InetSocketAddress(proxyHost, proxyPort)));
    }
    BufferingClientHttpRequestFactory bufferedRequestFactory = new BufferingClientHttpRequestFactory(requestFactory);
    restTemplate.setRequestFactory(bufferedRequestFactory);
    return restTemplate;
  }

  /**
   * Creates a {@link ApiClient}.
   *
   * @return the created client.
   */
  @Bean
  public ApiClient apiClient(NetworkConfig networkConfig) {
    ApiClient apiClient =
        new ApiClient(restTemplate()).setBasePath(String.format(BASE_URL_TEMPLATE, networkConfig.getApiRoot()));
    if (networkConfig.getAuthMethod().equals("basic")) {
      apiClient.setUsername(networkConfig.getUserName());
      apiClient.setPassword(networkConfig.getPassword());
    } else {
      apiClient.setAccessToken(networkConfig.getToken());
    }
    apiClient.setDebugging(networkConfig.getNetworkDebug());
    return apiClient;
  }

  @Bean
  public MongoMappingContext mongoMappingContext() {
    return new MongoMappingContext();
  }
}
