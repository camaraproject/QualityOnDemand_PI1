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

package com.camara.qod.repository;

import com.camara.qod.entity.H2QosSession;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for QoSSessions.
 */
@Repository
@Profile("local")
public interface QodSessionH2Repository extends JpaRepository<H2QosSession, UUID> {

  /**
   * Get QoS session by Device IPv4 address.
   *
   * @param ipAddr user equipment ip address to search for
   * @return QoS session or null if session not found
   */
  List<H2QosSession> findByDeviceIpv4addr(String ipAddr);

  /**
   * Get QoS session by SENF/NEF subscription.
   *
   * @param subscriptionId subscription id to search for
   * @return QoS session or null if session not found
   */
  Optional<H2QosSession> findBySubscriptionId(@NotBlank String subscriptionId);

  List<H2QosSession> findByExpiresAtLessThan(Long expirationTime);
}
