/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 - 2023 Contributors | Deutsche Telekom AG to CAMARA a Series of LF
 *             Projects, LLC
 * The contributor of this file confirms his sign-off for the
 * Developer
 *             Certificate of Origin (http://developercertificate.org).
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

import com.camara.qod.api.SessionsApiDelegate;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.service.QodService;
import com.camara.qod.service.ValidationService;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


/**
 * Contains implementations for the methods of the sessions' path.
 */
@Controller
@RequiredArgsConstructor
public class SessionsController implements SessionsApiDelegate {

  @Value("${qod.duration.default:600}")
  private int defaultDuration;
  private final QodService qodService;

  private final ValidationService validationService;

  /**
   * POST /sessions : Creates a new QoS session on demand.
   *
   * @param createSession Creates a new session (required)
   * @return Session created (status code 201) or Invalid input (status code 400) or Unauthorized (status code 401) or Forbidden (status
   *     code 403) or Conflict (status code 409) or Server error (status code 500) or Service unavailable (status code 503)
   */
  @Override
  public ResponseEntity<SessionInfo> createSession(CreateSession createSession) {
    createSession.setDuration(
        Optional.ofNullable(createSession.getDuration()).orElse(defaultDuration));
    validationService.validate(createSession);
    SessionInfo sessionInfo = qodService.createSession(createSession);

    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(sessionInfo.getId())
        .toUri();
    return ResponseEntity.created(location).body(sessionInfo);
  }

  /**
   * GET /sessions/{sessionId} : Get session information.
   *
   * @param sessionId Session ID that was obtained from the createSession operation (required)
   * @return Contains information about active session (status code 200) or Unauthorized (status code 401) or Forbidden (status code 403) or
   *     Session not found (status code 404) or Service unavailable (status code 503)
   */
  @Override
  public ResponseEntity<SessionInfo> getSession(UUID sessionId) {
    SessionInfo sessionInfo = qodService.getSession(sessionId);
    return ResponseEntity.status(HttpStatus.OK).body(sessionInfo);
  }

  /**
   * DELETE /sessions/{sessionId} : Free resources related to QoS session.
   *
   * @param sessionId Session ID that was obtained from the createSession operation (required)
   * @return Session deleted (status code 204) or Unauthorized (status code 401) or Forbidden (status code 403) or Session not found (status
   *     code 404) or Service unavailable (status code 503)
   */
  @Override
  public ResponseEntity<Void> deleteSession(UUID sessionId) {
    qodService.deleteSession(sessionId);
    return ResponseEntity.noContent().build();
  }
}
