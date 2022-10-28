/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 Contributors | Deutsche Telekom AG to CAMARA a Series of LF Projects, LLC
 * The contributor of this file confirms his sign-off for the
 * Developer Certificate of Origin (http://developercertificate.org).
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

package com.camara.qod.plugin.storage;

import com.camara.datatypes.model.QosSession;
import com.camara.datatypes.model.QosSessionIdWithExpiration;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.plugin.storage.mapping.StorageModelMapper;
import com.camara.qod.plugin.storage.model.RedisQosSession;
import com.camara.qod.plugin.storage.repository.QodSessionRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

@Component(value = "Storage")
@RequiredArgsConstructor
public class StorageImpl implements StorageInterface {

  private final RedisConfig redisConfig;
  private final RedisTemplate<String, String> redisTemplate;
  private final StorageModelMapper storageModelMapper;
  private final QodSessionRepository sessionRepo;

  @Override
  public RedisQosSession saveSession(long startedAt,
      long expiresAt,
      UUID uuid,
      CreateSession session,
      String subscriptionId,
      UUID bookkeeperId) {

    RedisQosSession redisQosSession =
        RedisQosSession.builder()
            .id(uuid)
            .startedAt(startedAt)
            .expiresAt(expiresAt)
            .duration(session.getDuration())
            .ueAddr(session.getUeAddr())
            .asAddr(session.getAsAddr())
            .uePorts(session.getUePorts())
            .asPorts(session.getAsPorts())
            .protocolIn(session.getProtocolIn())
            .protocolOut(session.getProtocolOut())
            .qos(session.getQos())
            .subscriptionId(subscriptionId)
            .notificationUri(session.getNotificationUri())
            .notificationAuthToken(session.getNotificationAuthToken())
            .expirationLockUntil(0) // Expiration Lock is initialised with 0, gets updated when an
            // ExpiredSessionTask is created
            .bookkeeperId(bookkeeperId)
            .build();
    sessionRepo.save(redisQosSession);

    return redisQosSession;
  }

  @Override
  public QosSession saveSession(QosSession qosSession) {
    return sessionRepo.save(storageModelMapper.mapToRedisQosSession(qosSession));
  }

  @Override
  public Optional<QosSession> getSession(UUID id) {
    return sessionRepo
        .findById(id)
        .map(storageModelMapper::mapToLibraryQosSession);
  }

  @Override
  public void deleteSession(UUID id) {
    sessionRepo.deleteById(id);
  }

  @Override
  public void addExpiration(UUID id, long expiresAt) {
    redisTemplate
        .opsForZSet()
        .add(
            redisConfig.getQosSessionExpirationListName(),
            id.toString(),
            expiresAt);
  }

  @Override
  public void removeExpiration(UUID id) {
    redisTemplate
        .opsForZSet()
        .remove(redisConfig.getQosSessionExpirationListName(), id.toString());
  }

  @Override
  public List<QosSessionIdWithExpiration> getSessionsThatExpireUntil(double expirationTime) {
    Set<ZSetOperations.TypedTuple<String>> qosSessionExpirationList =
        redisTemplate
            .opsForZSet()
            .rangeByScoreWithScores(
                redisConfig.getQosSessionExpirationListName(), 0, expirationTime);
    return qosSessionExpirationList != null ? qosSessionExpirationList.stream().map(storageModelMapper::mapToList)
        .collect(Collectors.toList()) : null;
  }

  @Override
  public List<QosSession> findByUeAddr(String ueAddr) {
    return sessionRepo
        .findByUeAddr(ueAddr)
        .stream()
        .map(storageModelMapper::mapToLibraryQosSession)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<QosSession> findBySubscriptionId(String subscriptionId) {
    return sessionRepo.findBySubscriptionId(subscriptionId).map(storageModelMapper::mapToLibraryQosSession);
  }

}
