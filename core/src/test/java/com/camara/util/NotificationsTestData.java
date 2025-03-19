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

package com.camara.util;

import com.camara.network.api.model.UserPlaneEvent;
import com.camara.network.api.model.UserPlaneEventReport;
import com.camara.network.api.model.UserPlaneNotificationData;
import java.util.List;

/**
 * This test data class provides useful session test data.
 */
public class NotificationsTestData extends TestData {

  public static final String NOTIFICATION_URI = "/3gpp-as-session-with-qos/v1/notifications";

  public static final String TEST_NOTIFICATION_REQUEST = """
      {
        "transaction": "123",
        "eventReports": [
          {
            "event": "SESSION_TERMINATION"
          }
        ]
      }
      """;

  public static final String TEST_NOTIFICATION_REQUEST_EMPTY_TRANSACTION = """
      {
        "transaction": "subscriptions/",
        "eventReports": [
          {
            "event": "SESSION_TERMINATION"
          }
        ]
      }
      """;

  /**
   * Creates a test {@link UserPlaneNotificationData}.
   *
   * @param event the {@link UserPlaneEvent}
   * @return the build request.
   */
  public static UserPlaneNotificationData createTestNotificationRequest(UserPlaneEvent event) {
    UserPlaneNotificationData userPlaneNotificationData = new UserPlaneNotificationData();
    userPlaneNotificationData.setTransaction("subscriptions/124");

    UserPlaneEventReport eventReport = new UserPlaneEventReport();
    eventReport.setEvent(event);

    userPlaneNotificationData.setEventReports(List.of(eventReport));
    return userPlaneNotificationData;
  }

}
