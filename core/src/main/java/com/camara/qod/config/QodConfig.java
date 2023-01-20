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

package com.camara.qod.config;

import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * This class contains specific configurations for Quality on Demand.
 */
@Configuration
@Getter
@ToString
public class QodConfig {

  public static final String NETWORK_SEGMENT_REGEX =
      "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])(?:\\.(?:[01]?\\d\\d?|2[0-4]\\d|25[0-5])){3}(\\/"
          + "([0-9]|[1-2][0-9]|3[0-2]))$";
  @Value("${qod.qos.references.qos-e}")
  private String qosReferenceQosE;
  @Value("${qod.qos.references.qos-s}")
  private String qosReferenceQosS;
  @Value("${qod.qos.references.qos-m}")
  private String qosReferenceQosM;
  @Value("${qod.qos.references.qos-l}")
  private String qosReferenceQosL;
  @Value("${qod.expiration.time-before-handling}")
  private int qosExpirationTimeBeforeHandling;
  @Value("${qod.expiration.trigger-interval}")
  private int qosExpirationTriggerInterval;
  @Value("${qod.expiration.lock-time}")
  private int qosExpirationLockTimeInSeconds;
  @Value("${qod.mask-sensible-data}")
  private Boolean qosMaskSensibleData;
  @Value("${qod.allow-multiple-ueaddr}")
  private Boolean qosAllowMultipleUeAddr;
  @Value("${qod.availability.enabled}")
  private Boolean qosAvailabilityEnabled;
  @Value("${qod.availability.url}")
  private String qosAvailabilityUrl;
}
