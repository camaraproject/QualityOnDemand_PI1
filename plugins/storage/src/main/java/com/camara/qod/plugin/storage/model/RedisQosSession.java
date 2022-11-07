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

package com.camara.qod.plugin.storage.model;

import com.camara.qod.api.model.AsId;
import com.camara.qod.api.model.PortsSpec;
import com.camara.qod.api.model.UeId;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

/**
 * This is the QoS Session subscription resource.
 */
@Data
@SuperBuilder
@RedisHash("QoSSession")
@NoArgsConstructor
public class RedisQosSession extends com.camara.datatypes.model.QosSession {

  @Indexed
  private String ueIpv4addr;
  @Indexed
  private UeId ueId;
  @Indexed
  private AsId asId;
  @Indexed
  private PortsSpec uePorts;
  @Indexed
  private PortsSpec asPorts;
}
