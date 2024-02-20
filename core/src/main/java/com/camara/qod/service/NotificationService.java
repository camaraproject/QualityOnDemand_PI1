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
import com.camara.qod.api.model.StatusInfo;
import com.camara.qod.model.QosSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationService {

  private final SessionService sessionService;
  private final StorageService storage;
  private final EventHubService eventHubService;

  /**
   * Handles the QoS notification.
   *
   * @param subscriptionId the subscriptionId
   * @param event          the {@link UserPlaneEvent}
   * @return {@link CompletableFuture}
   */
  @Async
  public CompletableFuture<Void> handleQosNotification(@NotBlank String subscriptionId, @NotNull UserPlaneEvent event) {
    Optional<QosSession> sessionOptional = storage.findBySubscriptionId(subscriptionId);
    if (sessionOptional.isPresent() && event.equals(UserPlaneEvent.SESSION_TERMINATION)) {
      QosSession session = sessionOptional.get();
      SessionInfo sessionInfo = sessionService.deleteSession(session.getSessionId());
      eventHubService.sendEvent(sessionInfo, StatusInfo.NETWORK_TERMINATED);
    }
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Updates the {@link QosStatus} of a session.
   *
   * @param subscriptionId the subscriptionId for the corresponding {@link QosSession}.
   * @param updatedStatus  the new {@link QosStatus}
   */
  public void setQosStatusForSession(String subscriptionId, QosStatus updatedStatus) {
    Optional<QosSession> sessionOptional = storage.findBySubscriptionId(subscriptionId);
    if (sessionOptional.isPresent() && sessionOptional.get().getQosStatus() != updatedStatus) {
      QosSession qosSession = sessionOptional.get();
      QosStatus oldStatus = qosSession.getQosStatus();
      qosSession.setQosStatus(updatedStatus);
      storage.saveSession(qosSession);
      log.debug("Status of QosSession was updated from <{}> to <{}>", oldStatus, updatedStatus);
    }
  }
}
