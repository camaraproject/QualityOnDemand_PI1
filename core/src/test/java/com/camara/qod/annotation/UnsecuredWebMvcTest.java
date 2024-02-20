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

package com.camara.qod.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AliasFor;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest(excludeFilters = {
    @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        value = WebSecurityConfigurer.class)
},
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
    })
public @interface UnsecuredWebMvcTest {

  /**
   * Alias for the {@code controllers} attribute of {@link org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest}. Specifies
   * the controller classes to be registered for the test slice.
   *
   * @return the controller classes to be registered for the test slice
   */
  @AliasFor(annotation = WebMvcTest.class, attribute = "controllers") Class<?>[] value() default {};

  /**
   * Alias for the {@code controllers} attribute of {@link org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest}. Specifies
   * the controller classes to be registered for the test slice.
   *
   * @return the controller classes to be registered for the test slice
   */
  @AliasFor(annotation = WebMvcTest.class, attribute = "controllers") Class<?>[] controllers() default {};
}
