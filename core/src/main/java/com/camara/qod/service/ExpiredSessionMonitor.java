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

import com.camara.qod.api.model.QosStatus;
import com.camara.qod.api.model.StatusInfo;
import com.camara.qod.config.QodConfig;
import com.camara.qod.model.QosSession;
import com.camara.qod.model.QosSessionIdWithExpiration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Takes care of sessions, that will soon expire or are already expired.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiredSessionMonitor {

  private final SessionService sessionService;
  private final QodConfig qodConfig;
  private final StorageService storage;

  /**
   * Setup expiration listener to check for (almost) expired sessions.
   */
  @Scheduled(fixedDelayString = "${qod.expiration.trigger-interval}000")
  @SchedulerLock(name = "expiredSessionMonitor")
  public void checkForExpiredSessions() {
    log.debug("Check for (almost) expired sessions...");
    long maxExpiration = Instant.now().getEpochSecond() + this.qodConfig.getQosExpirationTimeBeforeHandling();

    List<QosSessionIdWithExpiration> qosSessionExpirationList = getExpiringQosSessions(maxExpiration);
    log.debug("QoS sessions which will soon expire: {}", qosSessionExpirationList);

    if (qosSessionExpirationList != null) {
      qosSessionExpirationList.forEach(this::handleExpiredSession);
    }
  }

  private List<QosSessionIdWithExpiration> getExpiringQosSessions(long maxExpiration) {
    return storage.getSessionsThatExpireUntil(maxExpiration);
  }

  private void handleExpiredSession(QosSessionIdWithExpiration expiredQosSession) {
    UUID sessionId = expiredQosSession.getId();
    Optional<QosSession> sessionOptional = storage.getSession(sessionId);

    if (sessionOptional.isPresent()) {
      long now = Instant.now().getEpochSecond();
      QosSession session = sessionOptional.get();
      // Check that no valid lock exits:
      if (session.getExpirationLockUntil() < now) {
        log.info("Create ExpiredSessionTask for session with id: {}", sessionId);
        long scheduleAt = Math.max(expiredQosSession.getExpiresAt(), now);
        session.setExpirationLockUntil(scheduleAt + this.qodConfig.getQosExpirationLockTimeInSeconds());
        storage.saveSession(session);

        scheduleExpirationTask(sessionId, scheduleAt);
      }
    } else {
      log.warn("Session with ID <{}> is not existing and will be removed from Expiration list.", sessionId);
      storage.removeExpiration(sessionId);
    }
  }

  private void scheduleExpirationTask(UUID sessionId, long scheduleAt) {
    new Timer().schedule(new ExpiredSessionTask(sessionId), Date.from(Instant.ofEpochSecond(scheduleAt)));
  }

  /**
   * Class that deletes an expired session. Every almost expired session creates an instance of this class.
   */
  @RequiredArgsConstructor
  class ExpiredSessionTask extends TimerTask {

    private final UUID sessionId;

    @Override
    public void run() {
      log.info("QoD session {} expired, deleting...", sessionId);
      Optional<QosSession> session = storage.getSession(sessionId);
      StatusInfo statusInfo = determineStatusInfo(session);
      sessionService.deleteAndNotify(sessionId, statusInfo);
    }

    private static StatusInfo determineStatusInfo(Optional<QosSession> session) {
      StatusInfo statusInfo = StatusInfo.DURATION_EXPIRED;
      if (session.isPresent() && session.get().getQosStatus() == QosStatus.UNAVAILABLE) {
        statusInfo = StatusInfo.NETWORK_TERMINATED;
      }
      return statusInfo;
    }
  }
}
