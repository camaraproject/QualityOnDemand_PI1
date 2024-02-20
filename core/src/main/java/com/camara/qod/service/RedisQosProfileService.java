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

package com.camara.qod.service;

import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.QosProfileStatusEnum;
import com.camara.qod.entity.QosProfileRedisEntity;
import com.camara.qod.exception.QodApiException;
import com.camara.qod.mapping.QosProfileMapper;
import com.camara.qod.repository.QosProfilesRedisRepository;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Profile("!local")
@Slf4j
public class RedisQosProfileService implements QosProfileService {

  private final QosProfilesRedisRepository qosProfilesRedisRepository;

  private final QosProfileMapper qosProfileMapper;

  /**
   * Loads initial static data into Redis DB.
   */
  @PostConstruct
  public void initData() {
    qosProfilesRedisRepository.deleteAll();
    QosProfileRedisEntity profileE = createStaticActiveQosProfile("E");
    QosProfileRedisEntity profileS = createStaticActiveQosProfile("S");
    QosProfileRedisEntity profileM = createStaticActiveQosProfile("M");
    QosProfileRedisEntity profileL = createStaticActiveQosProfile("L");
    List<QosProfileRedisEntity> profiles = List.of(profileE, profileS, profileM, profileL);
    qosProfilesRedisRepository.saveAll(profiles);
  }

  /**
   * Retrieves a list of QoS profiles based on the provided name and status.
   *
   * @param name   The name of the QoS profile to search for. Can be null or empty to ignore.
   * @param status The status of the QoS profiles to search for. Can be null to ignore.
   * @return A list of QoS profiles matching the specified criteria.
   * @throws QodApiException If no matching QoS profiles are found.
   */
  public List<QosProfile> getQosProfiles(String name, QosProfileStatusEnum status) {
    List<QosProfileRedisEntity> profiles;
    if (StringUtils.isEmpty(name) && status == null) {
      profiles = qosProfilesRedisRepository.findAll();
    } else if (StringUtils.isEmpty(name)) {
      profiles = qosProfilesRedisRepository.findAllByStatus(status);
      log.info("{} QoS profile(s) found for status <{}>", profiles.size(), status);
    } else if (status == null) {
      profiles = Collections.singletonList(getQosProfileByName(name));
    } else {
      profiles = qosProfilesRedisRepository.findAllByNameAndStatus(name, status);
      log.info("{} QoS profile(s) found for name <{}> and status <{}>", profiles.size(), name, status);
    }
    if (profiles.isEmpty()) {
      throw new QodApiException(HttpStatus.NOT_FOUND, "No QoS Profiles found");
    }
    return qosProfileMapper.mapRedisToQosProfileList(profiles);
  }

  /**
   * Retrieves a single QoS profile based on the provided name.
   *
   * @param name The name of the QoS profile to retrieve.
   * @return The retrieved QoS profile.
   */
  public QosProfile getQosProfile(String name) {
    QosProfileRedisEntity foundProfile = getQosProfileByName(name);
    return qosProfileMapper.mapToQosProfile(foundProfile);
  }

  /**
   * Retrieves a QoS profile from the repository based on its name.
   *
   * @param name The name of the QoS profile to retrieve.
   * @return The QoS profile with the specified name.
   * @throws QodApiException If the QoS profile with the specified name is not found.
   */
  private QosProfileRedisEntity getQosProfileByName(String name) {
    return qosProfilesRedisRepository.findByName(name)
        .orElseThrow(() -> new QodApiException(HttpStatus.NOT_FOUND, "QosProfile Id does not exist"));
  }

  private static QosProfileRedisEntity createStaticActiveQosProfile(String profileIdentifier) {
    return QosProfileRedisEntity.builder()
        .name("QOS_" + profileIdentifier)
        .description("The QOS profile " + profileIdentifier)
        .status(QosProfileStatusEnum.ACTIVE)
        .build();
  }

}

