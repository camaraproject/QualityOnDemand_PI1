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

package com.camara.qod.mapping;

import com.camara.qod.api.model.QosProfile;
import com.camara.qod.entity.QosProfileH2Entity;
import com.camara.qod.entity.QosProfileRedisEntity;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * Maps the QosSession model to the SessionsInfo model.
 */
@Mapper(componentModel = "spring")
public interface QosProfileMapper {

  QosProfile mapToQosProfile(QosProfileH2Entity qosProfile);

  QosProfile mapToQosProfile(QosProfileRedisEntity qosProfile);


  List<QosProfile> mapH2ToQosProfileList(List<QosProfileH2Entity> qosProfile);

  List<QosProfile> mapRedisToQosProfileList(List<QosProfileRedisEntity> qosProfile);
}
