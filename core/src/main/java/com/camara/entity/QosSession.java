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

package com.camara.entity;

import com.camara.quality_on_demand.api.model.ApplicationServer;
import com.camara.quality_on_demand.api.model.Device;
import com.camara.quality_on_demand.api.model.PortsSpec;
import com.camara.quality_on_demand.api.model.QosStatus;
import com.camara.quality_on_demand.api.model.SinkCredential;
import com.camara.quality_on_demand.api.model.StatusInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * This is the QoS Session subscription resource.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@Getter
@Setter
@ToString
@Document("qos_sessions")
public class QosSession {

  @Id
  private String id;

  @Indexed
  private String clientId;

  @Indexed(unique = true)
  private String sessionId;

  @Indexed
  private String deviceIpv4addr;

  @Indexed
  private String subscriptionId;

  private String startedAt;

  @Indexed
  private String expiresAt;

  private int duration;

  private Device device;

  private ApplicationServer applicationServer;

  private PortsSpec devicePorts;

  private PortsSpec applicationServerPorts;

  private String qosProfile;

  private String sink;

  private SinkCredential sinkCredential;

  private QosStatus qosStatus;

  private StatusInfo statusInfo;

  @Default
  private boolean showDeviceInResponse = true;

  @Default
  private boolean isScheduledForDeletion = false;
}
