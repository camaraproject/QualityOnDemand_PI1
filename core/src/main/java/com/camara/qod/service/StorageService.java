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


import com.camara.qod.api.model.CreateSession;
import com.camara.qod.model.QosSession;
import com.camara.qod.model.QosSessionIdWithExpiration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageService {

  /**
   * Save QoS session.
   *
   * @param startedAt      - timestamp of session begin
   * @param expiresAt      - timestamp of automatic session termination
   * @param uuid           - id of session
   * @param session        - information to new session
   * @param subscriptionId - network QoS subscription ID
   * @return created QoS session
   */
  QosSession saveSession(long startedAt,
      long expiresAt,
      UUID uuid,
      CreateSession session,
      String subscriptionId,
      UUID bookkeeperId);

  QosSession saveSession(QosSession qosSession);

  Optional<QosSession> getSession(UUID id);

  void deleteSession(UUID id);

  void removeExpiration(UUID id);

  /**
   * Get QoS sessions with smaller expire time than the given time.
   *
   * @param expirationTime timestamp
   * @return list with sessions, that expire within the given time
   */
  List<QosSessionIdWithExpiration> getSessionsThatExpireUntil(Long expirationTime);

  List<QosSession> findByDeviceIpv4addr(String ipAddr);

  Optional<QosSession> findBySubscriptionId(String subscriptionId);
}
