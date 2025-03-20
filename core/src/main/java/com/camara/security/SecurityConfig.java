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

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(prefix = "enhanced-token-validation", name = "enabled", havingValue = "true")
@EnableWebSecurity
public class SecurityConfig {

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuerUri;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    var jwtIssuerAuthenticationManagerResolver = JwtIssuerAuthenticationManagerResolver.fromTrustedIssuers(issuerUri);

    http.authorizeHttpRequests(authorizeRequest -> {
      authorizeRequest.requestMatchers(antMatcher("/swagger-ui/**")).permitAll();
      authorizeRequest.requestMatchers(antMatcher("/swagger/**")).permitAll();
      authorizeRequest.requestMatchers(antMatcher("/v3/api-docs/**")).permitAll();
      authorizeRequest.requestMatchers(antMatcher("/actuator/**")).permitAll();
      authorizeRequest.requestMatchers(antMatcher("/qos-profiles.yaml")).permitAll();
      authorizeRequest.requestMatchers(antMatcher("/quality-on-demand.yaml")).permitAll();
      authorizeRequest.requestMatchers(antMatcher("/h2-console/*")).permitAll();
      authorizeRequest.requestMatchers(antMatcher("/**/notifications")).permitAll();
      authorizeRequest.anyRequest().authenticated();
    }).csrf(AbstractHttpConfigurer::disable).headers(
        httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
    http.oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(jwtIssuerAuthenticationManagerResolver));
    return http.build();
  }

}
