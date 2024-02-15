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

package com.camara.qod.entity;

import com.camara.qod.api.model.Duration;
import com.camara.qod.api.model.QosProfileStatusEnum;
import com.camara.qod.api.model.Rate;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@RedisHash("qos_profiles")
@NoArgsConstructor
@AllArgsConstructor
public class QosProfileRedisEntity {

  @Id
  private String id;
  @Indexed
  private String name;
  private String description;
  @Indexed
  private QosProfileStatusEnum status;

  private Rate targetMinUpstreamRate;

  private Rate maxUpstreamRate;

  private Rate maxUpstreamBurstRate;

  private Rate targetMinDownstreamRate;

  private Rate maxDownstreamRate;

  private Rate maxDownstreamBurstRate;

  private Duration minDuration;

  private Duration maxDuration;

  private Duration packetDelayBudget;

  private Duration jitter;

  private Integer priority;

  private Integer packetErrorLossRate;
}
