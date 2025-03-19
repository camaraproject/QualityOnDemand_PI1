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

package com.camara.util;

import com.camara.qos_profiles.api.model.Duration;
import com.camara.qos_profiles.api.model.QosProfile;
import com.camara.qos_profiles.api.model.QosProfileStatusEnum;
import com.camara.qos_profiles.api.model.TimeUnitEnum;
import java.util.ArrayList;
import java.util.List;

public class QosProfilesTestData extends TestData {

  public static final String RETRIEVE_QOS_PROFILES_URI = "/qos-profiles/v0.11/retrieve-qos-profiles";
  public static final String RETRIEVE_QOS_PROFILE_BY_NAME_URI = "/qos-profiles/v0.11/qos-profiles";

  public static final int PROFILE_MIN_DURATION = 10;
  public static final Duration minDuration = new Duration().value(PROFILE_MIN_DURATION).unit(TimeUnitEnum.SECONDS);
  public static final int PROFILE_MAX_DURATION = 86400;
  public static final Duration maxDuration = new Duration().value(PROFILE_MAX_DURATION).unit(TimeUnitEnum.SECONDS);

  /**
   * Gets a {@link com.camara.entity.QosProfile} by name.
   *
   * @param profileName the name of the profile
   * @return {@link com.camara.entity.QosProfile}
   */
  public static com.camara.entity.QosProfile getQosProfileEntity(String profileName) {
    List<com.camara.entity.QosProfile> allProfiles = getQosProfilesEntityTestData();
    return allProfiles.stream()
        .filter(qosProfile -> qosProfile.getName().equals(profileName))
        .findFirst()
        .orElse(null);
  }


  /**
   * Generates a list of test {@link QosProfile}.
   *
   * @return A list containing test QoS profiles with predefined properties.
   */
  public static List<QosProfile> getQosProfilesTestData() {
    List<QosProfile> qosProfiles = new ArrayList<>();

    qosProfiles.add(new QosProfile()
        .name("QOS_E")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile E")
        .maxDuration(maxDuration)
        .minDuration(minDuration));

    qosProfiles.add(new QosProfile()
        .name("QOS_S")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile S")
        .maxDuration(maxDuration)
        .minDuration(minDuration));

    qosProfiles.add(new QosProfile()
        .name("QOS_M")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile M")
        .maxDuration(maxDuration)
        .minDuration(minDuration));

    qosProfiles.add(new QosProfile()
        .name("QOS_L")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile L")
        .maxDuration(maxDuration)
        .minDuration(minDuration));

    return qosProfiles;
  }

  /**
   * Generates a list of test {@link com.camara.entity.QosProfile}.
   *
   * @return A list containing test QoS profiles entities with predefined properties.
   */
  public static List<com.camara.entity.QosProfile> getQosProfilesEntityTestData() {
    List<com.camara.entity.QosProfile> qosProfiles = new ArrayList<>();

    qosProfiles.add(com.camara.entity.QosProfile.builder()
        .name("QOS_E")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile E")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    qosProfiles.add(com.camara.entity.QosProfile.builder()
        .name("QOS_S")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile S")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    qosProfiles.add(com.camara.entity.QosProfile.builder()
        .name("QOS_M")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile M")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    qosProfiles.add(com.camara.entity.QosProfile.builder()
        .name("QOS_L")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile L")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    return qosProfiles;
  }
}
