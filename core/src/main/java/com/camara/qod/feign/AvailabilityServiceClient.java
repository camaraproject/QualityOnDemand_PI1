/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 - 2023 Contributors | Deutsche Telekom AG to CAMARA a Series of LF
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

package com.camara.qod.feign;

import com.camara.qod.model.AvailabilityRequest;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "avsClient", url = "${qod.availability.url}")
public interface AvailabilityServiceClient {

  @PostMapping(
      value = "/api/v1/sessions",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE)
  ResponseEntity<String> createSession(@Valid @RequestBody AvailabilityRequest availabilityRequest);

  @PostMapping(
      value = "/api/v1/sessions/check",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Void> checkSession(@Valid @RequestBody AvailabilityRequest availabilityRequest);

  @DeleteMapping(value = "/api/v1/sessions/{id}")
  ResponseEntity<Void> deleteSession(@PathVariable("id") UUID id);
}
