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

package com.camara.qod.config;

import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Network QoS API configuration, see <a href="http://www.3gpp.org/ftp/Specs/archive/29_series/29.122/">...</a>.
 */
@Configuration
@Getter
@ToString
public class NetworkConfig {

  @Value("${network.server.apiroot}")
  private String apiRoot;

  @Value("${network.server.flowid.qos-e}")
  private int flowIdQosE;

  @Value("${network.server.flowid.qos-s}")
  private int flowIdQosS;

  @Value("${network.server.flowid.qos-m}")
  private int flowIdQosM;

  @Value("${network.server.flowid.qos-l}")
  private int flowIdQosL;

  @Value("${network.server.scsasid}")
  private String scsAsId;

  @Value("${network.notifications.url}")
  private String networkNotificationsDestination;

  @Value("${network.auth.type}")
  private String authMethod;

  @Value("${network.auth.username}")
  private String userName;

  @Value("${network.auth.password}")
  private String password;

  @Value("${network.auth.oauth.token}")
  private String token;

  @Value("${network.auth.oauth2.token-endpoint}")
  private String tokenEndpoint;

  @Value("${network.auth.oauth2.client-id}")
  private String clientId;

  @Value("${network.auth.oauth2.client-secret}")
  private String clientSecret;

  @Value("${network.debug}")
  private Boolean networkDebug;

  @Value("${network.server.supportedFeatures}")
  private String supportedFeatures;

  @Value("${network.server.supported-event-type.resource-allocation:true}")
  private boolean supportedEventResourceAllocation;
}
