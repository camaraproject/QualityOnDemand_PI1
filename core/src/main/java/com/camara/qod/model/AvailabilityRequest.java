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

package com.camara.qod.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The AvailabilityRequest represents required parameters for creating or checking a session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityRequest {

  @Schema(description = "Availability session identifier.", example = "59d375b4-51e0-11ed-bdc3-0242ac120002")
  private UUID uuid;

  @Schema(description = "Start time point.", example = "2017-01-13T17:09:42.411")
  private Date startsAt;

  @Schema(description = "Expire time point.", example = "2017-01-13T17:09:42.411")
  private Date expiresAt;

  @Schema(description = "The profile qualifier.")
  private String qosProfile;
}
