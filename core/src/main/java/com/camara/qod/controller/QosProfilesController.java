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

package com.camara.qod.controller;

import com.camara.qod.api.QosProfilesApiDelegate;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.QosProfileStatusEnum;
import com.camara.qod.service.QosProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class QosProfilesController implements QosProfilesApiDelegate {

  private final QosProfileService qosProfileService;

  @Override
  public ResponseEntity<List<QosProfile>> getQosProfiles(String name, QosProfileStatusEnum status) {
    List<QosProfile> qosProfiles = qosProfileService.getQosProfiles(name, status);
    return ResponseEntity.ok(qosProfiles);
  }

  @Override
  public ResponseEntity<QosProfile> getQosProfile(String name) {
    QosProfile qosProfile = qosProfileService.getQosProfile(name);
    return ResponseEntity.ok(qosProfile);
  }
}
