/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 - 2023 Contributors | Deutsche Telekom AG to CAMARA a Series of LF
 *             Projects, LLC
 * The contributor of this file confirms his sign-off for the
 * Developer
 *             Certificate of Origin (http://developercertificate.org).
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

package com.camara.qod.service;

import com.camara.qod.api.model.CreateSession;
import com.camara.qod.exception.ErrorCode;
import com.camara.qod.exception.SessionApiException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import javax.validation.constraints.NotNull;

@Service
@RequiredArgsConstructor
public class ValidationService {

  /**
   * Validates a {@link CreateSession}.
   *
   * @param createSession the {@link CreateSession}
   */
  public void validate(@NotNull CreateSession createSession) {
    validateNetwork(createSession.getAsId().getIpv4addr());
    validateNetwork(createSession.getUeId().getIpv4addr());
  }

  /**
   * Checks if network is defined with the start address, e.g. 2001:db8:85a3:8d3:1319:8a2e:370:7344/128 and not
   * 2001:db8:85a3:8d3:1319:8a2e:370:7344/120.
   */
  private void validateNetwork(String network) {
    IPAddress current = new IPAddressString(network).getAddress();
    IPAddress rewritten = current.toPrefixBlock();
    if (current != rewritten) {
      throw new SessionApiException(
          HttpStatus.BAD_REQUEST, "Network specification not valid " + network, ErrorCode.VALIDATION_FAILED);
    }
  }

}
