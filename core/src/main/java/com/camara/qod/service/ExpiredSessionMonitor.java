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

package com.camara.qod.service;

import com.camara.datatypes.model.QosSession;
import com.camara.datatypes.model.QosSessionIdWithExpiration;
import com.camara.qod.api.model.SessionEvent;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.config.QodConfig;
import com.camara.qod.plugin.storage.StorageInterface;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Takes care of sessions, that will soon expire or are already expired.
 */
@Service
@RequiredArgsConstructor
public class ExpiredSessionMonitor {

  private static final Logger log = LoggerFactory.getLogger(ExpiredSessionMonitor.class);

  private final QodService qodService;
  private final QodConfig qodConfig;
  private final StorageInterface storage;

  /**
   * Setup expiration listener to check for (almost) expired sessions.
   */
  @Scheduled(fixedDelayString = "${qod.expiration.trigger-interval}000")
  @SchedulerLock(name = "expiredSessionMonitor")
  public void checkForExpiredSessions() {
    log.debug("Check for (almost) expired sessions...");
    double maxExpiration =
        Instant.now().getEpochSecond() + this.qodConfig.getQosExpirationTimeBeforeHandling();

    // Get QoS sessions with smaller expire time than the given time
    List<QosSessionIdWithExpiration> qosSessionExpirationList = storage.getSessionsThatExpireUntil(
        maxExpiration);
    log.debug("QoS sessions which will soon expire: " + qosSessionExpirationList);

    if (qosSessionExpirationList != null) {
      qosSessionExpirationList.forEach(
          expiredQosSession -> {
            Optional<QosSession> sessionOptional =
                storage.getSession(expiredQosSession.getId());
            if (sessionOptional.isPresent()) {
              long now = Instant.now().getEpochSecond();
              QosSession session = sessionOptional.get();
              // Check that no valid lock exits:
              if (session.getExpirationLockUntil() < now) {
                log.info(
                    "Create ExpiredSessionTask for session with id: "
                        + expiredQosSession.getId());
                long scheduleAt = Math.max(expiredQosSession.getExpiresAt(), now);
                // The lock ensures, that the task is only scheduled once. If the task fails, a new
                // task is scheduled afterwards.
                session.setExpirationLockUntil(
                    scheduleAt + this.qodConfig.getQosExpirationLockTimeInSeconds());
                storage.saveSession(session);

                // schedule expiration task:
                new Timer()
                    .schedule(
                        new ExpiredSessionTask(expiredQosSession.getId()),
                        Date.from(Instant.ofEpochSecond(scheduleAt)));
              }
            } else {
              log.warn("Session in Expiration list is not existing");
            }
          });
    }
  }

  /**
   * Class that deletes an expired session. Every almost expired session creates an instance of this
   * class.
   */
  class ExpiredSessionTask extends TimerTask {

    private final UUID sessionId;

    public ExpiredSessionTask(UUID sessionId) {
      this.sessionId = sessionId;
    }

    @Override
    public void run() {
      log.info("QoD session " + sessionId + " expired, deleting...");
      SessionInfo sessionInfo = qodService.deleteSession(sessionId);
      qodService.notifySession(sessionInfo, SessionEvent.SESSION_TERMINATED);
    }
  }
}
