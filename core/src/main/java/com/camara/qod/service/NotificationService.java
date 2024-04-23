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

import com.camara.network.api.model.UserPlaneEvent;
import com.camara.qod.api.model.QosStatus;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.config.QodConfig;
import com.camara.qod.mapping.SessionModelMapper;
import com.camara.qod.model.QosSession;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationService {

  private final QodConfig qodConfig;
  private final StorageService storage;
  private final EventHubService eventHubService;
  private final SessionModelMapper sessionModelMapper;


  /**
   * Handles the QoS notification.
   *
   * @param subscriptionId the subscriptionId
   */
  public void handleQosNotification(@NotBlank String subscriptionId, UserPlaneEvent event) {
    Optional<QosSession> sessionOptional = storage.findBySubscriptionId(subscriptionId);
    if (sessionOptional.isPresent()) {
      QosSession session = sessionOptional.get();
      switch (event) {
        case SESSION_TERMINATION, FAILED_RESOURCES_ALLOCATION -> handleNetworkTermination(session);
        case SUCCESSFUL_RESOURCES_ALLOCATION -> handleSuccessfulAllocation(session);
        default -> log.warn("Unhandled Notification Event <{}>", event);
      }
    } else {
      log.info("Callback Subscription-ID <{}> does not have a corresponding existing QoD-Session", subscriptionId);
    }
  }

  private void handleNetworkTermination(QosSession session) {
    long deletionDelay = qodConfig.getDeletionDelay();
    long updatedExpiration = Instant.now().plusSeconds(deletionDelay).getEpochSecond();
    session.setExpiresAt(updatedExpiration);
    session.setQosStatus(QosStatus.UNAVAILABLE);
    log.info("The Network has terminated the session. The session will be deleted in <{}> seconds.", deletionDelay);
    storage.saveSession(session);
  }

  private void handleSuccessfulAllocation(QosSession qosSession) {
    qosSession.setQosStatus(QosStatus.AVAILABLE);
    storage.saveSession(qosSession);
    log.debug("Status of QosSession was updated to <{}>", qosSession.getQosStatus());
    SessionInfo sessionInfo = sessionModelMapper.map(qosSession);
    eventHubService.sendEvent(sessionInfo);
  }
}
