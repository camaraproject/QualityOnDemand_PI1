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

package com.camara.service;

import com.camara.config.QodConfig;
import com.camara.exception.QodApiException;
import com.camara.mapping.QosProfileMapper;
import com.camara.qos_profiles.api.model.Duration;
import com.camara.qos_profiles.api.model.QosProfile;
import com.camara.qos_profiles.api.model.QosProfileStatusEnum;
import com.camara.repository.QosProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class QosProfileService {

  private final QosProfileRepository qosProfileRepository;

  private final QosProfileMapper qosProfileMapper;

  private final QodConfig qodConfig;

  /**
   * Loads the initial data-set for the qos-profiles.
   */
  @Generated
  @PostConstruct
  public void loadQosProfiles() {
    String initialDataSource = qodConfig.getInitialQosProfilesDataSource();
    try (InputStream inputStream = getClass().getResourceAsStream(initialDataSource)) {
      if (inputStream == null) {
        log.error("Resource '{}' not found", initialDataSource);
        return;
      }
      List<QosProfile> qosProfiles = new ObjectMapper().readValue(inputStream, new TypeReference<>() {});
      var entities = qosProfileMapper.mapToQosProfileEntities(qosProfiles);
      qosProfileRepository.deleteAll();
      qosProfileRepository.saveAll(entities);
      log.info("QOS profiles successfully loaded into MongoDB.");
    } catch (Exception e) {
      log.error("Failed to load QOS profiles: {}", e.getMessage());
    }
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
    List<com.camara.entity.QosProfile> profiles;
    if (StringUtils.isEmpty(name) && status == null) {
      profiles = qosProfileRepository.findAll();
    } else if (StringUtils.isEmpty(name)) {
      profiles = qosProfileRepository.findAllByStatus(status);
      log.info("{} QoS profile(s) found for status <{}>", profiles.size(), status);
    } else if (status == null) {
      profiles = Collections.singletonList(getQosProfileByName(name));
    } else {
      profiles = qosProfileRepository.findAllByNameAndStatus(name, status);
      log.info("{} QoS profile(s) found for name <{}> and status <{}>", profiles.size(), name, status);
    }
    if (profiles.isEmpty()) {
      throw new QodApiException(HttpStatus.NOT_FOUND, "No QoS Profiles found");
    }
    return qosProfileMapper.mapToQosProfileList(profiles);
  }

  /**
   * Retrieves a single QoS profile based on the provided name.
   *
   * @param name The name of the QoS profile to retrieve.
   * @return The retrieved QoS profile.
   */
  public QosProfile getQosProfile(String name) {
    var foundProfile = getQosProfileByName(name);
    return qosProfileMapper.mapToQosProfile(foundProfile);
  }

  /**
   * Maps the {@link Duration} into seconds.
   *
   * @param duration {@link Duration}
   * @return the value in seconds
   */
  public long retrieveDurationInSeconds(Duration duration) {
    long value = duration.getValue();
    return switch (duration.getUnit()) {
      case DAYS -> value * 86400;
      case HOURS -> value * 3600;
      case MINUTES -> value * 60;
      case SECONDS -> value;
      case MILLISECONDS -> value / 1000;
      case MICROSECONDS -> value / 1_000_000;
      case NANOSECONDS -> value / 1_000_000_000;
    };
  }

  /**
   * Retrieves a QoS profile from the repository based on its name.
   *
   * @param name The name of the QoS profile to retrieve.
   * @return The QoS profile with the specified name.
   * @throws QodApiException If the QoS profile with the specified name is not found.
   */
  private com.camara.entity.QosProfile getQosProfileByName(String name) {
    return qosProfileRepository.findByName(name)
        .orElseThrow(() -> new QodApiException(HttpStatus.NOT_FOUND, "QosProfile Id does not exist"));
  }

}
