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

package com.camara.exception;

public enum ErrorCode {
  DEVICE_NOT_FOUND,
  INTERNAL,
  INVALID_ARGUMENT,
  INVALID_PROTOCOL,
  INVALID_TOKEN_CONTEXT,
  INVALID_CREDENTIAL,
  NOT_ALLOWED,
  OUT_OF_RANGE,
  DURATION_OUT_OF_RANGE,
  PARAMETER_MISSING,
  QUALITY_ON_DEMAND,
  SESSION_EXTENSION_NOT_ALLOWED,
  VALIDATION_FAILED,
  UNIDENTIFIABLE_DEVICE,
  UNSUPPORTED_DEVICE_IDENTIFIERS;

  public static String getQualityOnDemandErrorCode(String subErrorCode) {
    return QUALITY_ON_DEMAND.name() + "." + subErrorCode;
  }

}
