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

package com.camara.qod.service;

import com.camara.network.api.ApiClient;
import com.camara.network.api.AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi;
import com.camara.network.api.AsSessionWithQoSApiSubscriptionLevelPostOperationApi;
import com.camara.network.api.model.AsSessionWithQoSSubscription;
import com.camara.network.api.model.FlowInfo;
import com.camara.network.api.model.ProblemDetails;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.Message;
import com.camara.qod.api.model.PortsSpec;
import com.camara.qod.api.model.PortsSpecRangesInner;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.commons.Util;
import com.camara.qod.config.NetworkConfig;
import com.camara.qod.config.QodConfig;
import com.camara.qod.exception.ErrorCode;
import com.camara.qod.exception.QodApiException;
import com.camara.qod.feign.AvailabilityServiceClient;
import com.camara.qod.mapping.SessionModelMapper;
import com.camara.qod.model.AvailabilityRequest;
import com.camara.qod.model.QosSession;
import com.camara.qod.model.SupportedQosProfiles;
import feign.FeignException;
import inet.ipaddr.IPAddressString;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Service that supports the implementations of the methods of the /sessions path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

  private static final int NUMBER_OF_UNMASKED_CHARS = 4;
  private static final int FLOW_ID_UNKNOWN = -1;
  private static final String FLOW_DESCRIPTION_TEMPLATE_IN = "permit in ip from %s to %s";
  private static final String FLOW_DESCRIPTION_TEMPLATE_OUT = "permit out ip from %s to %s";
  private static final String[] PRIVATE_NETWORKS = {"10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"};

  private static final String QOS_PROFILE_UNKNOWN_ERROR_MESSAGE = "QoS profile <%s> unknown or disabled";

  private static final String OAUTH2_CLIENT_CREDENTIALS_FLOW_AUTH = "oauth2-client-credentials-flow";

  public static final int SECONDS_PER_DAY = 86399;

  /**
   * Network QoS API, see <a href="http://www.3gpp.org/ftp/Specs/archive/29_series/29.122/">...</a>.
   */
  private final NetworkConfig networkConfig;
  private final QodConfig qodConfig;
  private final SessionModelMapper sessionModelMapper;
  private final StorageService storage;
  private final AsSessionWithQoSApiSubscriptionLevelPostOperationApi postApi;
  private final AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi deleteApi;
  private final AvailabilityServiceClient avsClient;
  private final ApiClient apiClient;
  private final NetworkAccessTokenExchanger networkAccessTokenExchanger;

  /**
   * Convert PortsSpec to NEF format.
   *
   * @param ports {@link PortsSpec}
   * @return ports in NEF format
   */
  private static String convertPorts(PortsSpec ports) {
    StringBuilder res = new StringBuilder();
    if (ports.getPorts() != null) {
      for (var port : ports.getPorts()) {
        res.append(port).append(",");
      }
    }
    if (ports.getRanges() != null) {
      for (var range : ports.getRanges()) {
        res.append(range.getFrom()).append("-").append(range.getTo()).append(",");
      }
    }
    return res.deleteCharAt(res.length() - 1).toString();
  }

  /**
   * Checks if there are no ports defined.
   *
   * @param ports the {@link PortsSpec}
   * @return true if there are no ports defined.
   */
  private static boolean isPortsSpecNotDefined(PortsSpec ports) {
    if (ports == null) {
      return true;
    }
    return ports.getPorts() == null && ports.getRanges() == null;
  }

  /**
   * Masks a string.
   *
   * @param unmaskedString string to mask
   * @return transformed unmaskedString so that each character from its beginning is replaced with X, only last numberOfUnmaskedChars
   *     characters won't be changed.
   */
  private static String maskString(String unmaskedString) {
    int indexOfMaskDelimiter = unmaskedString.length() - NUMBER_OF_UNMASKED_CHARS;
    return unmaskedString.substring(0, indexOfMaskDelimiter).replaceAll("[^-]", "X") + unmaskedString.substring(indexOfMaskDelimiter);
  }

  /**
   * Check if the given networks intersect.
   *
   * @param network1 single IP address or network
   * @param network2 single IP address or network
   * @return true for intersecting networks, false otherwise
   */
  private static boolean checkNetworkIntersection(String network1, String network2) {
    IPAddressString one = new IPAddressString(network1);
    IPAddressString two = new IPAddressString(network2);
    return one.contains(two) || two.contains(one);
  }

  /**
   * Checks if ports range is ordered from lower to higher & are in 0-65535.
   *
   * @param ports {@link PortsSpecRangesInner}
   */
  private static void checkPortRange(PortsSpec ports) {
    if (ports == null || ports.getRanges() == null) {
      return;
    }
    for (var portsSpecRanges : ports.getRanges()) {
      if (portsSpecRanges.getFrom() > portsSpecRanges.getTo()) {
        throw new QodApiException(HttpStatus.BAD_REQUEST,
            "Ports specification not valid, given range: from " + portsSpecRanges.getFrom() + ", to " + portsSpecRanges.getTo(),
            ErrorCode.VALIDATION_FAILED);
      }
    }
    if (ports.getPorts() != null) {
      Optional<Integer> maxPort = ports.getPorts().stream().max(Integer::compareTo);
      Optional<Integer> minPort = ports.getPorts().stream().min(Integer::compareTo);

      if (maxPort.isPresent() && minPort.isPresent() && (maxPort.get() > 65535 || minPort.get() < 0)) {
        throw new QodApiException(HttpStatus.BAD_REQUEST, "Ports ranges are not valid (0-65535)", ErrorCode.VALIDATION_FAILED);
      }
    }
  }

  /**
   * Check if the ports of a new requested session are already defined in an active Session.
   *
   * @param newSessionPorts Ports of the new requested Session
   * @param existingPorts   Ports of active sessions
   * @return true for ports which are already in use, false for ports not in use
   */
  public static boolean checkPortIntersection(PortsSpec newSessionPorts, PortsSpec existingPorts) {
    // ports list comparison
    if (newSessionPorts.getPorts() != null && existingPorts.getPorts() != null
        && newSessionPorts.getPorts().stream().anyMatch(existingPorts.getPorts()::contains)) {
      return true;
    }

    // ports list & ports range comparison
    if (checkPortRanges(newSessionPorts, existingPorts)) {
      return true;
    }
    if (checkPortRanges(existingPorts, newSessionPorts)) {
      return true;
    }

    // ports range comparison
    if (newSessionPorts.getRanges() != null && existingPorts.getRanges() != null) {
      for (var range : existingPorts.getRanges()) {
        if (newSessionPorts.getRanges().stream().anyMatch(port -> port.getTo() >= range.getFrom() && port.getFrom() <= range.getTo())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean checkPortRanges(PortsSpec portSpecs1, PortsSpec portSpecs2) {
    if (portSpecs1.getPorts() != null && portSpecs2.getRanges() != null) {
      for (var range : portSpecs2.getRanges()) {
        if (portSpecs1.getPorts().stream().anyMatch(port -> range.getFrom() <= port && range.getTo() >= port)) {
          return true;
        }
      }
    }
    return false;
  }

  @PostConstruct
  void setupApiClient() {
    postApi.setApiClient(apiClient);
    deleteApi.setApiClient(apiClient);
  }

  /**
   * Creates & saves session in database.
   */
  public SessionInfo createSession(@NotNull CreateSession session) {
    SupportedQosProfiles supportedQosProfile = SupportedQosProfiles.getProfileFromString(session.getQosProfile());
    final int flowId = getFlowId(supportedQosProfile);
    List<Message> messages = new ArrayList<>();
    String applicationServerIpv4Addr = session.getApplicationServer().getIpv4Address();
    String deviceIpv4Addr = session.getDevice().getIpv4Address().getPublicAddress();
    PortsSpec applicationServerPorts = session.getApplicationServerPorts();
    PortsSpec devicePorts = session.getDevicePorts();

    // Check if already exists
    Optional<QosSession> actual = checkExistingSessions(deviceIpv4Addr, applicationServerIpv4Addr,
        devicePorts, applicationServerPorts);
    if (actual.isPresent()) {
      QosSession s = actual.get();
      Instant expirationTime = Instant.ofEpochSecond(s.getExpiresAt());
      String sessionId = qodConfig.isQosMaskSensibleData() ? maskString(s.getSessionId().toString()) : s.getSessionId().toString();
      throw new QodApiException(HttpStatus.CONFLICT, "Found session " + sessionId + " already active until " + expirationTime);
    }
    // Check if asId.Ipv4Addr could be in private network
    for (String privateNetwork : PRIVATE_NETWORKS) {
      IPAddressString pn = new IPAddressString(privateNetwork);
      if (pn.contains(new IPAddressString(applicationServerIpv4Addr))) {
        String description = String.format("AS address range is in private network (%s). Some features may not work properly.",
            privateNetwork);
        Message warning = new Message(Message.SeverityEnum.WARNING, description);
        messages.add(warning);
        log.warn(description);
        break;
      }
    }

    String qosReference = getReference(supportedQosProfile);

    if (!isPortsSpecNotDefined(applicationServerPorts)) {
      // Validate port ranges generally beside the check in checkExistingSessions
      checkPortRange(applicationServerPorts);
      // AS port present. Append it to IP
      applicationServerIpv4Addr += " " + convertPorts(applicationServerPorts);
    }
    if (!isPortsSpecNotDefined(devicePorts)) {
      // Validate port ranges generally beside the check in checkExistingSessions
      checkPortRange(devicePorts);
      // UE port present. Append it to IP
      deviceIpv4Addr += " " + convertPorts(devicePorts);
    }

    // UUID for session
    UUID uuid = UUID.randomUUID();

    // Time
    long now = Instant.now().getEpochSecond();
    int duration = session.getDuration();
    long expiresAt = now + duration;

    // check if requested booking is available and book it
    final UUID bookkeeperId = qodConfig.isQosAvailabilityEnabled() ? createBooking(uuid, now, expiresAt, session) : null;
    FlowInfo flowInfo = createFlowInfo(deviceIpv4Addr, applicationServerIpv4Addr, flowId);
    AsSessionWithQoSSubscription qosSubscription = createQosSubscription(
        session.getDevice().getIpv4Address().getPublicAddress(),
        flowInfo,
        qosReference,
        networkConfig.getSupportedFeatures());

    if (OAUTH2_CLIENT_CREDENTIALS_FLOW_AUTH.equals(networkConfig.getAuthMethod())) {
      postApi.getApiClient().setAccessToken(networkAccessTokenExchanger.exchange());
    }
    AsSessionWithQoSSubscription response;
    try {
      response = postApi.scsAsIdSubscriptionsPost(networkConfig.getScsAsId(), qosSubscription);
    } catch (HttpStatusCodeException e) {
      ProblemDetails errorResponse = Util.extractProblemDetails(e);
      String errorMessage = errorResponse.getDetail() == null ? errorResponse.getCause() : errorResponse.getDetail();
      int httpStatusCode = e.getStatusCode().value();
      throw new QodApiException(HttpStatus.valueOf(httpStatusCode),
          "NEF/SCEF returned error " + httpStatusCode + " while creating a subscription on NEF/SCEF: " + errorMessage);
    }
    String subscriptionId = Util.extractSubscriptionId(response.getSelf());
    if (subscriptionId == null) {
      throw new QodApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No valid subscription ID was provided in NEF/SCEF response");
    }
    log.info("Save QoS session {}", session);
    QosSession qosSession = storage.saveSession(now, expiresAt, uuid, session, subscriptionId, bookkeeperId);

    SessionInfo ret = sessionModelMapper.map(qosSession);

    // Messages are only present in response but not in repository
    ret.setMessages(messages);
    return ret;
  }

  /**
   * Finds existing session by id.
   */
  public SessionInfo getSession(@NotNull UUID sessionId) {
    return storage.getSession(sessionId).map(sessionModelMapper::map)
        .orElseThrow(() -> new QodApiException(HttpStatus.NOT_FOUND, getSessionNotFoundMessage(sessionId)));
  }

  /**
   * Finds & removes session from the database.
   */
  public SessionInfo deleteSession(@NotNull UUID sessionId) {
    QosSession qosSession = storage.getSession(sessionId)
        .orElseThrow(() -> new QodApiException(HttpStatus.NOT_FOUND, getSessionNotFoundMessage(sessionId)));

    if (qodConfig.isQosAvailabilityEnabled()) {
      try {
        avsClient.deleteSession(qosSession.getBookkeeperId());
      } catch (FeignException e) {
        throw new QodApiException(HttpStatus.SERVICE_UNAVAILABLE, "The availability service is currently not available");
      }
    }

    // delete the session
    log.info("Delete QoS session {}", qosSession);
    storage.deleteSession(sessionId);

    if (qosSession.getSubscriptionId() != null) {
      try {
        if (OAUTH2_CLIENT_CREDENTIALS_FLOW_AUTH.equals(networkConfig.getAuthMethod())) {
          postApi.getApiClient().setAccessToken(networkAccessTokenExchanger.exchange());
        }
        deleteApi.scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(networkConfig.getScsAsId(), qosSession.getSubscriptionId());
      } catch (HttpClientErrorException.NotFound e) {
        log.error("NEF/SCEF reported a HTTP - Not Found while deleting subscription ID for session ID <{}>", sessionId);
        log.error("Problem by calling NEF/SCEF (Possibly already deleted by NEF): <{}>", e.getMessage());
      } catch (HttpStatusCodeException e) {
        ProblemDetails errorResponse = Util.extractProblemDetails(e);
        String errorMessage = errorResponse.getDetail() == null ? errorResponse.getCause() : errorResponse.getDetail();
        int httpStatusCode = e.getStatusCode().value();
        throw new QodApiException(HttpStatus.valueOf(httpStatusCode),
            "NEF/SCEF returned error " + httpStatusCode + " while deleting subscription on NEF/SCEF using subscription ID "
                + qosSession.getSubscriptionId() + ": " + errorMessage);
      } catch (Exception ex) {
        String errorMessage = "Unexpected exception occurred during deletion of subscription";
        log.error(errorMessage, ex);
        throw new QodApiException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
      }
    }
    return sessionModelMapper.map(qosSession);
  }

  /**
   * Extends the duration of a Quality of Service (QoS) session identified by the given session ID.
   *
   * @param sessionId          The unique identifier of the QoS session to be extended. Must not be null.
   * @param additionalDuration The additional duration (in seconds) by which the QoS session is to be extended. Must not be null.
   * @return A {@link SessionInfo} object representing the extended QoS session.
   * @throws QodApiException If the QoS session with the specified session ID is not found. The exception includes a 404 NOT FOUND HTTP
   *                         status and an error message.
   */
  public SessionInfo extendQosSession(@NotNull UUID sessionId, @NotNull Integer additionalDuration) {
    QosSession qosSession = storage.getSession(sessionId)
        .orElseThrow(() -> new QodApiException(HttpStatus.NOT_FOUND, getSessionNotFoundMessage(sessionId)));

    // ExpiredSessionMonitor -> check session handling in case of session will be expired next
    if (qosSession.getExpirationLockUntil() > 0) {
      throw new QodApiException(HttpStatus.NOT_FOUND,
          "The Quality of Service (QoD) session has reached its expiration, and the deletion process is running.");
    }

    // Calculate the new duration and expiresAt params by adding the additional duration
    var oldDuration = qosSession.getDuration();
    var oldStartedAt = qosSession.getStartedAt();
    var newDuration = oldDuration + additionalDuration;
    var newExpiresAt = oldStartedAt + oldDuration + additionalDuration;

    // Ensure the new duration does not exceed the maximum seconds per day
    if (newDuration > SECONDS_PER_DAY) {
      newDuration = SECONDS_PER_DAY;
      newExpiresAt = oldStartedAt + SECONDS_PER_DAY;
      log.info("The maximum extension of the duration has been exceeded, the limited maximum number of seconds per day <{}> will be used",
          SECONDS_PER_DAY);
    }

    log.info("Extended QoS session duration from {} to {} seconds.", oldDuration, newDuration);
    qosSession.setDuration(newDuration);
    qosSession.setExpiresAt(newExpiresAt);

    log.info("Save extended QoS session {}", qosSession);
    // Save the extended QoS session in storage
    qosSession = storage.saveSession(qosSession);
    return sessionModelMapper.map(qosSession);
  }

  /**
   * Generates a formatted error message for a session-not-found-scenario.
   *
   * @param sessionId The ID of the session that was not found.
   * @return A formatted error message indicating that the QoD session was not found for the provided session ID.
   */
  private static String getSessionNotFoundMessage(UUID sessionId) {
    return "QoD session not found for session ID: " + sessionId;
  }

  /**
   * Checks if the requested size is available, if so it is booked.
   *
   * @param uuid      the UUID of the booking.
   * @param now       the current time
   * @param expiresAt the time when the booking expires
   * @param session   {@link CreateSession}
   * @return the created uuid
   */
  private UUID createBooking(UUID uuid, long now, long expiresAt, CreateSession session) {

    var dateNow = Date.from(Instant.ofEpochSecond(now));
    var dateExpire = Date.from(Instant.ofEpochSecond(expiresAt));
    var qosProfile = session.getQosProfile();
    var request = AvailabilityRequest.builder()
        .uuid(uuid)
        .startsAt(dateNow)
        .expiresAt(dateExpire)
        .qosProfile(qosProfile)
        .build();

    try {
      avsClient.checkSession(request);
      var responseBody = avsClient.createSession(request).getBody();
      assert responseBody != null;
      return UUID.fromString(responseBody);
    } catch (FeignException e) {
      log.error("Problem by calling availability service: ", e);
      throw new QodApiException(HttpStatus.SERVICE_UNAVAILABLE, "The availability service is currently not available");
    }
  }

  /**
   * Return the value for the corresponding {@link QosProfile}.
   *
   * @param qosProfile the {@link QosProfile}
   * @return flow id from resources.
   */
  private int getFlowId(SupportedQosProfiles qosProfile) {
    int flowId = switch (qosProfile) {
      case QOS_E -> networkConfig.getFlowIdQosE();
      case QOS_S -> networkConfig.getFlowIdQosS();
      case QOS_M -> networkConfig.getFlowIdQosM();
      case QOS_L -> networkConfig.getFlowIdQosL();
    };

    if (flowId == FLOW_ID_UNKNOWN) {
      throw new QodApiException(HttpStatus.BAD_REQUEST,
          String.format(QOS_PROFILE_UNKNOWN_ERROR_MESSAGE, qosProfile),
          ErrorCode.VALIDATION_FAILED);
    }
    return flowId;
  }

  /**
   * Looks for existing sessions with the same ipv4 address, if existing network or ports intersect with the given parameters, returns
   * non-empty QosSession.
   *
   * @param ueAddr  the user equipment address
   * @param asAddr  the application server address
   * @param uePorts the user equipment ports
   * @param asPorts the application server ports
   * @return the {@link QosSession} as an {@link Optional}
   */
  private Optional<QosSession> checkExistingSessions(String ueAddr, String asAddr, PortsSpec uePorts, PortsSpec asPorts) {

    List<QosSession> qosSessions = storage.findByDeviceIpv4addr(ueAddr);

    return qosSessions.stream()
        .filter(qosSession -> checkNetworkIntersection(asAddr, qosSession.getApplicationServer().getIpv4Address()))
        .filter(qosSession -> checkPortIntersection(
            isPortsSpecNotDefined(uePorts)
                ? new PortsSpec().ranges(Collections.singletonList(new PortsSpecRangesInner().from(0).to(65535)))
                : uePorts, (isPortsSpecNotDefined(qosSession.getDevicePorts())) ? new PortsSpec().ranges(
                Collections.singletonList(new PortsSpecRangesInner().from(0).to(65535))) : qosSession.getDevicePorts()))
        .filter(qosSession -> checkPortIntersection(
            isPortsSpecNotDefined(asPorts)
                ? new PortsSpec().ranges(Collections.singletonList(new PortsSpecRangesInner().from(0).to(65535)))
                : asPorts, (isPortsSpecNotDefined(qosSession.getApplicationServerPorts())) ? new PortsSpec().ranges(
                Collections.singletonList(new PortsSpecRangesInner().from(0).to(65535))) : qosSession.getApplicationServerPorts()))
        .findFirst();
  }

  /**
   * Returns the reference to the corresponding QosProfile.
   *
   * @param qosProfile the QosProfile
   * @return reference from resources.
   */
  private String getReference(SupportedQosProfiles qosProfile) {
    return switch (qosProfile) {
      case QOS_E -> qodConfig.getQosReferenceQosE();
      case QOS_S -> qodConfig.getQosReferenceQosS();
      case QOS_M -> qodConfig.getQosReferenceQosM();
      case QOS_L -> qodConfig.getQosReferenceQosL();
    };
  }

  /**
   * Creates flow info.
   *
   * @param ueAddr UE ipv4 address + UE ports
   * @param asAddr AS ipv4 address + AS ports
   * @param flowId the flowId
   * @see SessionService#getFlowId
   */
  private FlowInfo createFlowInfo(String ueAddr, String asAddr, Integer flowId) {
    return new FlowInfo().flowId(flowId)
        .addFlowDescriptionsItem(String.format(FLOW_DESCRIPTION_TEMPLATE_IN, ueAddr, asAddr))
        .addFlowDescriptionsItem(String.format(FLOW_DESCRIPTION_TEMPLATE_OUT, asAddr, ueAddr));
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
  private AsSessionWithQoSSubscription createQosSubscription(String ueAddr, FlowInfo flowInfo, String qosReference,
      String supportedFeatures) {
    return new AsSessionWithQoSSubscription().ueIpv4Addr(ueAddr).flowInfo(List.of(flowInfo)).qosReference(qosReference)
        .notificationDestination(networkConfig.getNetworkNotificationsDestination()).requestTestNotification(true)
        .supportedFeatures(supportedFeatures);
  }
}
