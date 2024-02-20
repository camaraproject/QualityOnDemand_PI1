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
import com.camara.qod.entity.H2QosSession;
import com.camara.qod.mapping.StorageModelMapper;
import com.camara.qod.model.QosSession;
import com.camara.qod.model.QosSessionIdWithExpiration;
import com.camara.qod.repository.QodSessionH2Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("local")
public class H2StorageService implements StorageService {

  private final StorageModelMapper storageModelMapper;
  private final QodSessionH2Repository sessionRepo;
  private final NetworkConfig networkConfig;

  @Override
  public QosSession saveSession(long startedAt, long expiresAt, UUID uuid, CreateSession session, String subscriptionId,
      UUID bookkeeperId) {

    var qosStatus = networkConfig.isSupportedEventResourceAllocation() ? QosStatus.REQUESTED : QosStatus.AVAILABLE;

    var webhook = session.getWebhook();
    var notificationUrl = webhook != null ? webhook.getNotificationUrl() : null;
    var notificationAuthToken = webhook != null ? webhook.getNotificationAuthToken() : null;

    H2QosSession h2QosSession =
        H2QosSession.builder()
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
            .expirationLockUntil(0) // Expiration Lock is initialised with 0, gets updated when an
            // ExpiredSessionTask is created
            .bookkeeperId(bookkeeperId)
            .build();
    sessionRepo.save(h2QosSession);
    return storageModelMapper.mapToLibraryQosSession(h2QosSession);
  }

  @Override
  public QosSession saveSession(QosSession qosSession) {
    H2QosSession h2QosSession = storageModelMapper.mapToH2QosSession(qosSession);
    sessionRepo.save(h2QosSession);
    return storageModelMapper.mapToLibraryQosSession(h2QosSession);
  }

  @Override
  public Optional<QosSession> getSession(UUID id) {
    return sessionRepo.findById(id).map(storageModelMapper::mapToLibraryQosSession);
  }

  @Override
  public void deleteSession(UUID id) {
    sessionRepo.deleteById(id);
  }

  @Generated
  @Override
  public void removeExpiration(UUID id) {
    // not implemented
  }

  @Override
  public List<QosSessionIdWithExpiration> getSessionsThatExpireUntil(Long expirationTime) {
    List<H2QosSession> qosSessionExpirationList = sessionRepo.findByExpiresAtLessThan(expirationTime);
    return storageModelMapper.mapToList(qosSessionExpirationList);
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
