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

import com.camara.qod.entity.H2QosSession;
import com.camara.qod.entity.RedisQosSession;
import com.camara.qod.model.QosSession;
import com.camara.qod.model.QosSessionIdWithExpiration;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * Maps the QosSession model to the SessionsInfo model.
 */
@Mapper(componentModel = "spring")
public interface StorageModelMapper {

  //Redis Operations
  @Mapping(source = "sessionId", target = "id")
  RedisQosSession mapToRedisQosSession(QosSession qosSession);

  //H2 Operations
  @Mapping(source = "sessionId", target = "id")
  H2QosSession mapToH2QosSession(QosSession qosSession);

  @Mapping(source = "id", target = "sessionId")
  QosSession mapToLibraryQosSession(H2QosSession redisQosSession);

  @Mapping(source = "id", target = "sessionId")
  QosSession mapToLibraryQosSession(RedisQosSession redisQosSession);

  @Mapping(target = "id", expression = "java(java.util.UUID.fromString(redisSet.getValue()))")
  @Mapping(target = "expiresAt", expression = "java(redisSet.getScore().longValue())")
  QosSessionIdWithExpiration mapToList(ZSetOperations.TypedTuple<String> redisSet);

  List<QosSessionIdWithExpiration> mapToList(List<H2QosSession> h2QosSessions);

}
