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
import com.camara.qod.api.model.QosStatus;
import com.camara.qod.config.NetworkConfig;
import com.camara.qod.entity.RedisQosSession;
import com.camara.qod.mapping.StorageModelMapper;
import com.camara.qod.model.QosSession;
import com.camara.qod.model.QosSessionIdWithExpiration;
import com.camara.qod.repository.QodSessionRedisRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("!local")
public class RedisStorageService implements StorageService {

  private static final String QOS_SESSION_EXPIRATION_LIST_NAME = "QoSSessionExpirationList";

  private final RedisTemplate<String, String> redisTemplate;
  private final StorageModelMapper storageModelMapper;
  private final QodSessionRedisRepository sessionRepo;
  private final NetworkConfig networkConfig;

  @Override
  public QosSession saveSession(long startedAt, long expiresAt, UUID uuid, CreateSession session, String subscriptionId,
      UUID bookkeeperId) {
    var qosStatus = networkConfig.isSupportedEventResourceAllocation() ? QosStatus.REQUESTED : QosStatus.AVAILABLE;

    RedisQosSession redisQosSession = buildRedisQosSession(startedAt, expiresAt, uuid, session, subscriptionId, qosStatus, bookkeeperId);
    sessionRepo.save(redisQosSession);
    addExpiration(redisQosSession.getId(), redisQosSession.getExpiresAt());

    return storageModelMapper.mapToLibraryQosSession(redisQosSession);
  }

  @Override
  public QosSession saveSession(QosSession qosSession) {
    RedisQosSession redisQosSession = sessionRepo.save(storageModelMapper.mapToRedisQosSession(qosSession));
    // add an entry to a sorted set which contains the session id and the expiration time
    addExpiration(redisQosSession.getId(), redisQosSession.getExpiresAt());
    return storageModelMapper.mapToLibraryQosSession(redisQosSession);
  }

  private RedisQosSession buildRedisQosSession(long startedAt, long expiresAt, UUID uuid, CreateSession session, String subscriptionId,
      QosStatus qosStatus, UUID bookkeeperId) {
    var webhook = session.getWebhook();
    var notificationUrl = (webhook != null) ? webhook.getNotificationUrl() : null;
    var notificationAuthToken = (webhook != null) ? webhook.getNotificationAuthToken() : null;

    return RedisQosSession.builder()
        .id(uuid)
        .startedAt(startedAt)
        .expiresAt(expiresAt)
        .duration(session.getDuration())
        .deviceIpv4addr(session.getDevice().getIpv4Address().getPublicAddress())
        .device(session.getDevice())
        .applicationServer(session.getApplicationServer())
        .devicePorts(session.getDevicePorts())
        .applicationServerPorts(session.getApplicationServerPorts())
        .qosProfile(session.getQosProfile())
        .qosStatus(qosStatus)
        .subscriptionId(subscriptionId)
        .notificationUrl(notificationUrl)
        .notificationAuthToken(notificationAuthToken)
        .expirationLockUntil(0) // Expiration Lock is initialized with 0, gets updated when an
        .bookkeeperId(bookkeeperId)
        .build();
  }

  @Override
  public Optional<QosSession> getSession(UUID id) {
    return sessionRepo.findById(id).map(storageModelMapper::mapToLibraryQosSession);
  }

  @Override
  public void deleteSession(UUID id) {
    sessionRepo.deleteById(id);
    // delete the entry of the sorted set (id, expirationTime)
    removeExpiration(id);
  }

  private void addExpiration(UUID id, long expiresAt) {
    redisTemplate.opsForZSet().add(QOS_SESSION_EXPIRATION_LIST_NAME, id.toString(), expiresAt);
  }

  @Override
  public void removeExpiration(UUID id) {
    redisTemplate.opsForZSet().remove(QOS_SESSION_EXPIRATION_LIST_NAME, id.toString());
  }

  @Override
  public List<QosSessionIdWithExpiration> getSessionsThatExpireUntil(Long expirationTime) {
    Set<ZSetOperations.TypedTuple<String>> qosSessionExpirationList = redisTemplate.opsForZSet()
        .rangeByScoreWithScores(QOS_SESSION_EXPIRATION_LIST_NAME, 0, expirationTime);
    return qosSessionExpirationList != null ? qosSessionExpirationList.stream().map(storageModelMapper::mapToList)
        .toList() : null;
  }

  @Override
  public List<QosSession> findByDeviceIpv4addr(String ipAddr) {
    return sessionRepo
        .findByDeviceIpv4addr(ipAddr)
        .stream()
        .map(storageModelMapper::mapToLibraryQosSession)
        .toList();
  }

  @Override
  public Optional<QosSession> findBySubscriptionId(String subscriptionId) {
    return sessionRepo.findBySubscriptionId(subscriptionId).map(storageModelMapper::mapToLibraryQosSession);
  }

}
