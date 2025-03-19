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


import com.camara.qos_profiles.api.model.QosProfile;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Maps the QosSession model to the SessionsInfo model.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QosProfileMapper {

  QosProfile mapToQosProfile(com.camara.entity.QosProfile qosProfile);

  List<QosProfile> mapToQosProfileList(List<com.camara.entity.QosProfile> qosProfile);

  List<com.camara.entity.QosProfile> mapToQosProfileEntities(List<QosProfile> qosProfile);

}
