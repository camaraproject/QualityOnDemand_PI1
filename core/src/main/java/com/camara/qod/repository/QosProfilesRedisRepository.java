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

package com.camara.qod.repository;


import com.camara.qod.api.model.QosProfileStatusEnum;
import com.camara.qod.entity.QosProfileRedisEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Profile("!local")
@Repository
public interface QosProfilesRedisRepository extends ListCrudRepository<QosProfileRedisEntity, String> {

  Optional<QosProfileRedisEntity> findByName(String name);

  List<QosProfileRedisEntity> findAllByNameAndStatus(String name, QosProfileStatusEnum status);

  List<QosProfileRedisEntity> findAllByStatus(QosProfileStatusEnum status);
}
