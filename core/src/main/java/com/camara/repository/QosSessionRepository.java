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

package com.camara.repository;

import com.camara.entity.QosSession;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for QoSSessions.
 */
@Repository
public interface QosSessionRepository extends MongoRepository<QosSession, String> {

  Optional<QosSession> findBySessionId(String sessionId);

  List<QosSession> findAllByClientId(String clientId);

  void deleteBySessionId(String sessionId);

  /**
   * Get QoS session by Device IPv4 address.
   *
   * @param ipAddr user equipment ip address to search for
   * @return QoS session or null if session not found
   */
  List<QosSession> findByDeviceIpv4addr(String ipAddr);

  /**
   * Get QoS session by NEF subscription.
   *
   * @param subscriptionId subscription id to search for
   * @return QoS session or null if session not found
   */
  Optional<QosSession> findBySubscriptionId(@NotBlank String subscriptionId);

}
