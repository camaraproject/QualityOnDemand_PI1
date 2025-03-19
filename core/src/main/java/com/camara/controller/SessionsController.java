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

package com.camara.controller;

import com.camara.quality_on_demand.api.QoSSessionsApiDelegate;
import com.camara.quality_on_demand.api.model.CreateSession;
import com.camara.quality_on_demand.api.model.ExtendSessionDuration;
import com.camara.quality_on_demand.api.model.RetrieveSessionsInput;
import com.camara.quality_on_demand.api.model.SessionInfo;
import com.camara.quality_on_demand.api.model.StatusInfo;
import com.camara.service.SessionService;
import com.camara.service.ValidationService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


/**
 * Contains implementations for the methods of the sessions' path.
 */
@Controller
@RequiredArgsConstructor
public class SessionsController implements QoSSessionsApiDelegate {

  private final SessionService sessionService;
  private final ValidationService validationService;

  /**
   * POST /sessions: Creates a new QoS session on demand.
   *
   * @param createSession Creates a new session (required)
   * @param correlationId   Correlation id for the different services
   * @return Session created (status code 201) or Invalid input (status code 400) or Unauthorized (status code 401) or Forbidden (status
   *     code 403) or Conflict (status code 409) or Server error (status code 500) or Service unavailable (status code 503)
   */
  @Override
  public ResponseEntity<SessionInfo> createSession(CreateSession createSession, String correlationId) {
    boolean showDeviceInResponse = createSession.getDevice() != null;
    validationService.validate(createSession);
    SessionInfo sessionInfo = sessionService.createSession(createSession, showDeviceInResponse);

    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(sessionInfo.getSessionId())
        .toUri();
    return ResponseEntity.created(location).body(sessionInfo);
  }

  /**
   * DELETE /sessions/{sessionId}: Free resources related to QoS session.
   *
   * @param sessionId   Session ID that was obtained from the createSession operation (required)
   * @param correlationId Correlation id for the different services
   * @return Session deleted (status code 204) or Unauthorized (status code 401) or Forbidden (status code 403) or Session not found (status
   *     code 404) or Service unavailable (status code 503)
   */
  @Override
  public ResponseEntity<Void> deleteSession(UUID sessionId, String correlationId) {
    //Check if a session exists for this sessionId and the caller. It will fail otherwise.
    sessionService.getSessionInfoById(sessionId);
    sessionService.deleteAndNotify(sessionId.toString(), StatusInfo.DELETE_REQUESTED);
    return ResponseEntity.noContent().build();
  }

  /**
   * POST /sessions/{sessionId}/extend: Extend the duration of an active QoS session.
   *
   * @param sessionId             Session ID that was obtained from the createSession operation (required)
   * @param extendSessionDuration Parameters to extend the duration of an active session (required)
   * @param correlationId           Correlation id for the different services
   * @return Contains information about active session (status code 200) or Unauthorized (status code 401) or Forbidden (status code 403) or
   *     Session not found (status code 404) or Service unavailable (status code 503)
   */
  @Override
  public ResponseEntity<SessionInfo> extendQosSessionDuration(UUID sessionId, ExtendSessionDuration extendSessionDuration,
      String correlationId) {
    SessionInfo sessionInfo = sessionService.extendQosSession(sessionId, extendSessionDuration.getRequestedAdditionalDuration());
    return ResponseEntity.status(HttpStatus.OK).body(sessionInfo);
  }

  /**
   * POST /retrieve-sessions: Get session information for a device.
   *
   * @param retrieveSessionsInput Parameters to get QoS session information by device
   * @param correlationId           Correlation id for the different services
   * @return the QoS sessions information for a given device. A device may have multiple sessions, thus the response is an array. An empty
   *     array is returned if no sessions are found.
   */
  @Override
  public ResponseEntity<List<SessionInfo>> retrieveSessionsByDevice(RetrieveSessionsInput retrieveSessionsInput, String correlationId) {
    validationService.validate(retrieveSessionsInput);
    var sessionInfoList = sessionService.getSessionsByDevice(retrieveSessionsInput.getDevice());
    return ResponseEntity.status(HttpStatus.OK).body(sessionInfoList);
  }

  /**
   * GET /sessions/{sessionId} : Get session information.
   *
   * @param sessionId Session ID that was obtained from the createSession operation (required)
   * @return Contains information about active session (status code 200) or Unauthorized (status code 401) or Forbidden (status code 403) or
   *     Session not found (status code 404) or Service unavailable (status code 503)
   */
  @Override
  public ResponseEntity<SessionInfo> getSession(UUID sessionId, String correlationId) {
    SessionInfo sessionInfo = sessionService.getSessionInfoById(sessionId);
    return ResponseEntity.status(HttpStatus.OK).body(sessionInfo);
  }
}
