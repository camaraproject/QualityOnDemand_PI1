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

package com.camara.qod.model;

import com.camara.qod.api.model.ApplicationServer;
import com.camara.qod.api.model.Device;
import com.camara.qod.api.model.Message;
import com.camara.qod.api.model.PortsSpec;
import com.camara.qod.api.model.QosStatus;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * This is the QoS Session subscription resource.
 */
@Generated
@Data
@SuperBuilder
@NoArgsConstructor
public class QosSession {

  private String deviceIpv4addr;
  private UUID sessionId;
  private String subscriptionId;
  private Long startedAt;
  private Long expiresAt;
  private int duration;
  private Device device;
  private ApplicationServer applicationServer;
  private PortsSpec devicePorts;
  private PortsSpec applicationServerPorts;
  private String qosProfile;
  private URI notificationUrl;
  private String notificationAuthToken;
  private long expirationLockUntil; // The lock ensures that the task is only scheduled once for expiration.
  private UUID bookkeeperId;
  private QosStatus qosStatus;
  private List<Message> messages;
}
