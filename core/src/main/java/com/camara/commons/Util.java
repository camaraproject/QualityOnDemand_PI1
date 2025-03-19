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

package com.camara.commons;

import com.camara.exception.QodApiException;
import com.camara.network.api.model.ProblemDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * This class contains utility functions.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Util {

  /**
   * Returns the subscription id of a given subscription URI.
   *
   * @param subscriptionUri URI of the subscription
   * @return subscriptionId
   */
  public static String extractSubscriptionId(String subscriptionUri) {
    if (subscriptionUri == null) {
      return null;
    }
    String[] split = subscriptionUri.split("subscriptions/");
    if (split.length == 0) {
      return null;
    }
    return split[split.length - 1];
  }

  /**
   * Extracts the {@link ProblemDetails} from the response.
   *
   * @param error the {@link HttpStatusCodeException}
   * @return the extracted value for {@link ProblemDetails}
   */
  public static ProblemDetails extractProblemDetails(HttpStatusCodeException error) {
    try {
      String responseBodyAsString = error.getResponseBodyAsString();
      return new ObjectMapper().readValue(responseBodyAsString, ProblemDetails.class);
    } catch (JsonProcessingException j) {
      throw new QodApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while reading the response body of NEF");
    }
  }
}
