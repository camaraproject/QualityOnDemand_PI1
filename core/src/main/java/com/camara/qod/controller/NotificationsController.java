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

package com.camara.qod.controller;

import com.camara.network.api.model.UserPlaneEventReport;
import com.camara.network.api.model.UserPlaneNotificationData;
import com.camara.network.api.notifications.NotificationsApi;
import com.camara.network.api.notifications.NotificationsApiDelegate;
import com.camara.qod.commons.Util;
import com.camara.qod.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/**
 * Contains implementations for the methods of the notifications' path.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationsController implements NotificationsApiDelegate {

  private final NotificationService notificationService;

  /**
   * POST /notifications: notify bearer level event(s) from the SCEF to the SCS/AS.
   *
   * @see NotificationsApi#notificationsPost
   */
  @Override
  public ResponseEntity<Void> notificationsPost(
      UserPlaneNotificationData userPlaneNotificationData) {
    log.info("received notification");
    log.info(userPlaneNotificationData.toString());

    String subscriptionId = Util.extractSubscriptionId(userPlaneNotificationData.getTransaction());
    List<@Valid UserPlaneEventReport> eventReports = userPlaneNotificationData.getEventReports();

    for (UserPlaneEventReport report : eventReports) {
      notificationService.handleQosNotification(subscriptionId, report.getEvent());
    }
    return ResponseEntity.noContent().build();
  }
}
