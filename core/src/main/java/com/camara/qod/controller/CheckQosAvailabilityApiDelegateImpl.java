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

package com.camara.qod.controller;

import com.camara.qod.api.CheckQosAvailabilityApiDelegate;
import com.camara.qod.api.model.CheckQosAvailabilityResponse;
import com.camara.qod.api.model.CheckQosAvailabilityRequest;
import com.camara.qod.service.QodService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/** Contains implementations for the methods of the checkservicequalification' path. */
@Controller
public class CheckQosAvailabilityApiDelegateImpl implements CheckQosAvailabilityApiDelegate {
  private final QodService qodService;

  @Autowired
  public CheckQosAvailabilityApiDelegateImpl(QodService qodService) {
    this.qodService = qodService;
  }

  @Override
  public ResponseEntity<CheckQosAvailabilityResponse> checkQosAvailability(
      CheckQosAvailabilityRequest request) {

    CheckQosAvailabilityResponse response = qodService.checkQosAvailability(request.getUeId());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
