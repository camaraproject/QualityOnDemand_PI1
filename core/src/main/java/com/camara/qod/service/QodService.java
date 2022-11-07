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

import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.SessionEvent;
import com.camara.qod.api.model.SessionInfo;
import com.camara.scef.api.model.UserPlaneEvent;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.scheduling.annotation.Async;

/**
 * Service interface, that is used for the implementations of the methods of the sessions' path.
 */
public interface QodService {

  /**
   * Creates & saves session in database.
   */
  SessionInfo createSession(@NotNull CreateSession session);

  /**
   * Finds existing session by id.
   */
  SessionInfo getSession(@NotNull UUID sessionId);

  /**
   * Finds & removes session from database.
   */
  SessionInfo deleteSession(@NotNull UUID sessionId);

  /**
   * Handles the QoS notification.
   */
  @Async
  CompletableFuture<Void> handleQosNotification(@NotBlank String subscriptionId, @NotNull UserPlaneEvent event);

  /**
   * Takes care of expired sessions.
   */
  @Async
  CompletableFuture<Void> notifySession(@NotNull SessionInfo qosSession, @NotNull SessionEvent event);

}
