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

import com.camara.entity.QosSession;
import com.camara.quality_on_demand.api.model.QosStatus;
import com.camara.quality_on_demand.api.model.StatusInfo;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.collections.CollectionUtils;
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

  /**
   * Setup expiration listener to check for (almost) expired sessions.
   */
  @Scheduled(fixedDelayString = "${qod.expiration.trigger-interval}000")
  @SchedulerLock(name = "expiredSessionMonitor")
  public void checkForExpiredSessions() {
    log.debug("Check for (almost) expired sessions...");

    List<QosSession> qosSessionExpirationList = sessionService.getExpiringQosSessions();

    if (CollectionUtils.isNotEmpty(qosSessionExpirationList)) {
      log.info("QoS sessions which will soon expire: {}",
          qosSessionExpirationList.stream()
              .map(QosSession::getSessionId)
              .toList());
      qosSessionExpirationList.forEach(qosSession -> {
        scheduleExpirationTask(qosSession);
        qosSession.setScheduledForDeletion(true);
        sessionService.save(qosSession);
      });
    }
  }

  private void scheduleExpirationTask(QosSession expiredQosSession) {
    var expiresAt = OffsetDateTime.parse(expiredQosSession.getExpiresAt());
    new Timer().schedule(new ExpiredSessionTask(expiredQosSession), Date.from(expiresAt.toInstant()));
  }

  /**
   * Class that deletes an expired session. Every almost expired session creates an instance of this class.
   */
  @RequiredArgsConstructor
  class ExpiredSessionTask extends TimerTask {

    private final QosSession session;

    @Override
    public void run() {
      var sessionId = session.getSessionId();
      log.info("QoD session {} expired, deleting...", sessionId);
      StatusInfo statusInfo = determineStatusInfo(session);
      sessionService.deleteAndNotify(sessionId, statusInfo);
    }

    private static StatusInfo determineStatusInfo(QosSession session) {
      return session.getQosStatus() == QosStatus.UNAVAILABLE
          ? StatusInfo.NETWORK_TERMINATED
          : StatusInfo.DURATION_EXPIRED;
    }
  }
}
