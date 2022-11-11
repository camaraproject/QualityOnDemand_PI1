/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 Contributors | Deutsche Telekom AG to CAMARA a Series of LF
 *             Projects, LLC
 * The contributor of this file confirms his sign-off for the
 * Developer
 *             Certificate of Origin (http://developercertificate.org).
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

import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

  @Bean
  public Docket swagger() {
    return new Docket(SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.any())
        .paths(PathSelectors.any())
        .build();
  }

  @Primary
  @Bean
  public SwaggerResourcesProvider swaggerResourcesProvider() {
    return () -> {
      List<SwaggerResource> resources = new ArrayList<>();
      List.of("qod-api")
          .forEach(resourceName -> resources.add(loadResource(resourceName)));
      return resources;
    };
  }

  private SwaggerResource loadResource(String resource) {
    SwaggerResource wsResource = new SwaggerResource();
    wsResource.setName(resource);
    wsResource.setSwaggerVersion("2.0");
    wsResource.setLocation(resource + ".yaml");
    return wsResource;
  }
}
