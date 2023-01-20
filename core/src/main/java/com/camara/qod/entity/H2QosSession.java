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

package com.camara.qod.entity;

import com.camara.qod.api.model.AsId;
import com.camara.qod.api.model.PortsSpec;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.UeId;
import com.camara.qod.util.AsIdConverter;
import com.camara.qod.util.PortSpecConverter;
import com.camara.qod.util.UeIdConverter;
import java.net.URI;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This is the QoS Session subscription resource.
 */
@Getter
@Setter
@ToString
@SuperBuilder
@Entity
@NoArgsConstructor
@Table(name = "QoSSession", indexes = @Index(columnList = "ueIpv4addr, ueId, asId, uePorts, asPorts"))
public class H2QosSession {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;
  private String ueIpv4addr;
  private String subscriptionId;
  private Long startedAt;
  private Long expiresAt;
  private int duration;

  @Convert(converter = UeIdConverter.class)
  private UeId ueId;

  @Convert(converter = AsIdConverter.class)
  private AsId asId;

  @Convert(converter = PortSpecConverter.class)
  private PortsSpec uePorts;

  @Convert(converter = PortSpecConverter.class)
  private PortsSpec asPorts;

  private QosProfile qos;
  private URI notificationUri;
  private String notificationAuthToken;
  private long expirationLockUntil; // The lock ensures, that the task is only scheduled once for expiration.
  private UUID bookkeeperId;

}
