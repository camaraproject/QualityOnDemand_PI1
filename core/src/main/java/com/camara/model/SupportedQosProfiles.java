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

package com.camara.model;

import com.camara.exception.QodApiException;
import org.springframework.http.HttpStatus;

public enum SupportedQosProfiles {

  QOS_E,
  QOS_S,
  QOS_M,
  QOS_L;

  /**
   * Converts a string representation of a QoS profile into the corresponding {@link SupportedQosProfiles} enum value.
   *
   * @param qosProfile The string representation of the QoS profile.
   * @return The corresponding {@link SupportedQosProfiles} enum value if the string matches, otherwise throws an exception.
   * @throws QodApiException If the provided QoS profile string does not match any of the enum constants.
   */
  public static SupportedQosProfiles getProfileFromString(String qosProfile) {
    try {
      return valueOf(qosProfile);
    } catch (IllegalArgumentException ex) {
      throw new QodApiException(HttpStatus.BAD_REQUEST, "Unsupported QosProfile provided: " + qosProfile);
    }
  }
}
