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

package com.camara.mapping;

import com.camara.entity.QosSession;
import com.camara.quality_on_demand.api.model.SessionInfo;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps the CreateSession model to the SessionsInfo model.
 */
@Mapper(componentModel = "spring")
public interface SessionModelMapper {

  @Mapping(target = "device", expression = "java(qosSession.isShowDeviceInResponse() ? qosSession.getDevice() : null)")
  SessionInfo map(QosSession qosSession);

  /**
   * Converts a {@code String} representation of an {@code OffsetDateTime} to an {@code OffsetDateTime} object.
   *
   * @param offsetDateTimeAsString the {@code String} representation of the {@code OffsetDateTime}
   * @return the converted {@code OffsetDateTime} object, or {@code null} if the input string is {@code null}
   */
  default OffsetDateTime toOffsetDateTime(String offsetDateTimeAsString) {
    return offsetDateTimeAsString != null
        ? OffsetDateTime.parse(offsetDateTimeAsString)
        : null;
  }

}
