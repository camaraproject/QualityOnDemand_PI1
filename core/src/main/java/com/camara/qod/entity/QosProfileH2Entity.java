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
import com.camara.qod.util.DurationConverter;
import com.camara.qod.util.RateConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder
@Entity
@NoArgsConstructor
@Table(name = "qos_profiles")
public class QosProfileH2Entity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true)
  private String name;

  private String description;

  @Enumerated(EnumType.STRING)
  private QosProfileStatusEnum status;

  @Convert(converter = RateConverter.class)
  private Rate targetMinUpstreamRate;

  @Convert(converter = RateConverter.class)
  private Rate maxUpstreamRate;

  @Convert(converter = RateConverter.class)
  private Rate maxUpstreamBurstRate;

  @Convert(converter = RateConverter.class)
  private Rate targetMinDownstreamRate;

  @Convert(converter = RateConverter.class)
  private Rate maxDownstreamRate;

  @Convert(converter = RateConverter.class)
  private Rate maxDownstreamBurstRate;

  @Convert(converter = DurationConverter.class)
  private Duration minDuration;

  @Convert(converter = DurationConverter.class)
  private Duration maxDuration;

  @Convert(converter = DurationConverter.class)
  private Duration packetDelayBudget;

  @Convert(converter = DurationConverter.class)
  private Duration jitter;

  private Integer priority;

  private Integer packetErrorLossRate;
}
