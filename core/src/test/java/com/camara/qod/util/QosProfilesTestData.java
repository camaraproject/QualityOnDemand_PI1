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

package com.camara.qod.util;

import com.camara.qod.api.model.Duration;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.QosProfileStatusEnum;
import com.camara.qod.api.model.TimeUnitEnum;
import com.camara.qod.entity.QosProfileH2Entity;
import com.camara.qod.entity.QosProfileRedisEntity;
import java.util.ArrayList;
import java.util.List;

public class QosProfilesTestData extends TestData {

  public static final String QOS_PROFILES_URI = "/qod/v0/qos-profiles";
  public static final String STATUS_PARAMETER = "status";
  public static final String NAME_PARAMETER = "name";

  public static final int PROFILE_MIN_DURATION = 10;
  public static final Duration minDuration = new Duration().value(PROFILE_MIN_DURATION).unit(TimeUnitEnum.SECONDS);
  public static final int PROFILE_MAX_DURATION = 86400;
  public static final Duration maxDuration = new Duration().value(PROFILE_MAX_DURATION).unit(TimeUnitEnum.SECONDS);

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
   * Generates a list of test {@link QosProfileH2Entity}.
   *
   * @return A list containing test QoS profiles entities with predefined properties.
   */
  public static List<QosProfileH2Entity> getQosProfilesEntityTestData() {
    List<QosProfileH2Entity> qosProfiles = new ArrayList<>();

    qosProfiles.add(QosProfileH2Entity.builder()
        .name("QOS_E")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile E")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    qosProfiles.add(QosProfileH2Entity.builder()
        .name("QOS_S")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile S")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    qosProfiles.add(QosProfileH2Entity.builder()
        .name("QOS_M")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile M")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    qosProfiles.add(QosProfileH2Entity.builder()
        .name("QOS_L")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile L")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    return qosProfiles;
  }

  /**
   * Generates a list of test {@link QosProfileRedisEntity}.
   *
   * @return A list containing test QoS profiles entities with predefined properties.
   */
  public static List<QosProfileRedisEntity> getQosProfilesRedisEntityTestData() {
    List<QosProfileRedisEntity> qosProfiles = new ArrayList<>();

    qosProfiles.add(QosProfileRedisEntity.builder()
        .name("QOS_E")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile E")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    qosProfiles.add(QosProfileRedisEntity.builder()
        .name("QOS_S")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile S")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    qosProfiles.add(QosProfileRedisEntity.builder()
        .name("QOS_M")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile M")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    qosProfiles.add(QosProfileRedisEntity.builder()
        .name("QOS_L")
        .status(QosProfileStatusEnum.ACTIVE)
        .description("The QOS profile L")
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build());

    return qosProfiles;
  }

  /**
   * Generates a single object of test {@link QosProfileRedisEntity}.
   *
   * @return A single QoS profile entity with predefined properties.
   */
  public static QosProfileRedisEntity getSingleQosProfileRedisEntity(String qosProfileName) {
    return QosProfileRedisEntity.builder()
        .name(qosProfileName)
        .maxDuration(maxDuration)
        .minDuration(minDuration)
        .build();
  }
}
