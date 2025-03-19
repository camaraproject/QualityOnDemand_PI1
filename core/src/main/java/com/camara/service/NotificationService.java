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
import com.camara.entity.QosSession;
import com.camara.mapping.SessionModelMapper;
import com.camara.network.api.model.UserPlaneEvent;
import com.camara.quality_on_demand.api.model.QosStatus;
import com.camara.quality_on_demand.api.model.SessionInfo;
import com.camara.quality_on_demand.api.model.StatusInfo;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationService {

  private final QodConfig qodConfig;
  private final SessionService sessionService;
  private final EventHubService eventHubService;
  private final SessionModelMapper sessionModelMapper;

  /**
   * Handles the QoS notification.
   *
   * @param subscriptionId the subscriptionId
   */
  public void handleQosNotification(@NotBlank String subscriptionId, UserPlaneEvent event) {
    Optional<QosSession> sessionOptional = sessionService.findBySubscriptionId(subscriptionId);
    if (sessionOptional.isPresent()) {
      QosSession session = sessionOptional.get();
      if (session.isScheduledForDeletion()) {
        log.warn("The session with id <{}> is already locked for deletion. Notification callback will be skipped.", session.getSessionId());
        return;
      }
      switch (event) {
        case SESSION_TERMINATION, FAILED_RESOURCES_ALLOCATION -> handleNetworkTermination(session);
        case SUCCESSFUL_RESOURCES_ALLOCATION -> handleSuccessfulAllocation(session);
        default -> log.warn("Unhandled Notification Event <{}>", event);
      }
    } else {
      log.warn("Callback Subscription-ID <{}> does not have a corresponding existing QoD-Session", subscriptionId);
    }
  }

  private void handleNetworkTermination(QosSession session) {
    var deletionDelay = qodConfig.getDeletionDelay();
    var updatedExpiration = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(deletionDelay);
    session.setExpiresAt(updatedExpiration.toString());
    session.setQosStatus(QosStatus.UNAVAILABLE);
    session.setStatusInfo(StatusInfo.NETWORK_TERMINATED);
    log.info("The Network has terminated the session. The session will be deleted in <{}> seconds.", deletionDelay);
    sessionService.save(session);
  }

  private void handleSuccessfulAllocation(QosSession qosSession) {
    QosStatus currentSessionStatus = qosSession.getQosStatus();
    if (currentSessionStatus.equals(QosStatus.REQUESTED)) {
      OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);
      qosSession.setStartedAt(now.toString());
      qosSession.setExpiresAt(now.plusSeconds(qosSession.getDuration()).toString());
      qosSession.setQosStatus(QosStatus.AVAILABLE);
      sessionService.save(qosSession);
      log.info("QosSession with sessionId <{}> is now available.", qosSession.getSessionId());
      SessionInfo sessionInfo = sessionModelMapper.map(qosSession);
      eventHubService.sendEvent(sessionInfo);
    } else {
      log.info("Network reported with SUCCESSFUL_RESOURCES_ALLOCATION, but session is in status <{}>. No update.", currentSessionStatus);
    }

  }
}
