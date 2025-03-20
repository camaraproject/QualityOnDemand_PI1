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

package com.camara.service;

import com.camara.commons.Util;
import com.camara.config.NetworkConfig;
import com.camara.exception.QodApiException;
import com.camara.network.api.ApiClient;
import com.camara.network.api.AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi;
import com.camara.network.api.AsSessionWithQoSApiSubscriptionLevelPostOperationApi;
import com.camara.network.api.model.AsSessionWithQoSSubscription;
import com.camara.network.api.model.FlowInfo;
import com.camara.network.api.model.ProblemDetails;
import com.camara.quality_on_demand.api.model.CreateSession;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

@Service
@Slf4j
@RequiredArgsConstructor
public class NetworkService {

  private static final String OAUTH2_CLIENT_CREDENTIALS_FLOW_AUTH = "oauth2-client-credentials-flow";
  private static final String PERMANENT_FAILURES_ERROR = "Permanent Failures";
  private final ApiClient apiClient;
  private final AsSessionWithQoSApiSubscriptionLevelPostOperationApi postApi;
  private final AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi deleteApi;
  private final NetworkAccessTokenExchanger networkAccessTokenExchanger;
  private final NetworkConfig networkConfig;

  @Generated
  @PostConstruct
  void setupApiClient() {
    postApi.setApiClient(apiClient);
    deleteApi.setApiClient(apiClient);
  }

  /**
   * Creating a subscription on the network.
   *
   * @param session      the session information from the request
   * @param flowInfo     {@link FlowInfo}
   * @param qosReference the referenced qos
   * @return {@link AsSessionWithQoSSubscription}
   */
  public AsSessionWithQoSSubscription createQosSubscription(CreateSession session, FlowInfo flowInfo, String qosReference) {
    AsSessionWithQoSSubscription qosSubscription = buildQosSubscriptionRequest(
        session.getDevice().getIpv4Address().getPublicAddress(),
        flowInfo,
        qosReference,
        networkConfig.getSupportedFeatures());

    AsSessionWithQoSSubscription response;
    try {
      authorize();
      response = postApi.scsAsIdSubscriptionsPost(networkConfig.getScsAsId(), qosSubscription);
    } catch (HttpStatusCodeException e) {
      throw handleHttpStatusCodeException(e);
    }
    return response;
  }

  /**
   * Deletes a QoS - subscription on the network based on the subscriptionId.
   *
   * @param subscriptionId the subscription ID on the network
   */
  public void deleteNetworkSubscriptionById(String subscriptionId) {
    try {
      authorize();
      deleteApi.scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(networkConfig.getScsAsId(), subscriptionId);
    } catch (HttpClientErrorException.NotFound e) {
      log.error("NEF/SCEF reported a HTTP - Not Found while deleting subscription ID");
      log.error("Problem by calling NEF/SCEF (Possibly already deleted by NEF): <{}>", e.getMessage());
    } catch (HttpStatusCodeException e) {
      throw handleHttpStatusCodeException(e);
    }
  }

  /**
   * Creates Qos Subscription.
   *
   * @param ueAddr            the user equipment address
   * @param flowInfo          the {@link FlowInfo}
   * @param qosReference      the qos reference
   * @param supportedFeatures the supported features
   * @return {@link AsSessionWithQoSSubscription}
   */
  private AsSessionWithQoSSubscription buildQosSubscriptionRequest(String ueAddr, FlowInfo flowInfo, String qosReference,
      String supportedFeatures) {
    return new AsSessionWithQoSSubscription()
        .ueIpv4Addr(ueAddr)
        .flowInfo(List.of(flowInfo))
        .qosReference(qosReference)
        .notificationDestination(networkConfig.getNetworkNotificationsDestination())
        .requestTestNotification(true)
        .supportedFeatures(supportedFeatures);
  }


  /**
   * Handles {@link HttpStatusCodeException} by logging the error and throwing a {@link QodApiException} with the relevant information.
   *
   * @param e The {@link HttpStatusCodeException} to be handled.
   * @return The {@link QodApiException} containing the error details.
   */
  private QodApiException handleHttpStatusCodeException(HttpStatusCodeException e) {
    int httpStatusCode = e.getStatusCode().value();
    String errorMessage = "NEF/SCEF returned error " + httpStatusCode + " while calling NEF/SCEF";
    ProblemDetails errorResponse = Util.extractProblemDetails(e);
    if (errorResponse != null) {
      String nefErrorMessage = errorResponse.getDetail() == null ? errorResponse.getCause() : errorResponse.getDetail();
      if (nefErrorMessage.contains(PERMANENT_FAILURES_ERROR)) {
        nefErrorMessage = "Probably unknown IPv4 address for UE";
      }
      errorMessage = errorMessage.concat(": " + nefErrorMessage);
    }
    return new QodApiException(HttpStatus.valueOf(httpStatusCode), errorMessage);
  }

  @Generated
  private void authorize() {
    if (OAUTH2_CLIENT_CREDENTIALS_FLOW_AUTH.equals(networkConfig.getAuthMethod())) {
      postApi.getApiClient().setAccessToken(networkAccessTokenExchanger.exchange());
    }
  }
}
