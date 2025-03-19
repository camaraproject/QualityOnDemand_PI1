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

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import com.camara.commons.Util;
import com.camara.config.NetworkConfig;
import com.camara.config.QodConfig;
import com.camara.entity.QosSession;
import com.camara.exception.ErrorCode;
import com.camara.exception.QodApiException;
import com.camara.mapping.SessionModelMapper;
import com.camara.model.SupportedQosProfiles;
import com.camara.network.api.model.AsSessionWithQoSSubscription;
import com.camara.network.api.model.FlowInfo;
import com.camara.qos_profiles.api.model.QosProfile;
import com.camara.quality_on_demand.api.model.CreateSession;
import com.camara.quality_on_demand.api.model.Device;
import com.camara.quality_on_demand.api.model.PortsSpec;
import com.camara.quality_on_demand.api.model.PortsSpecRangesInner;
import com.camara.quality_on_demand.api.model.QosStatus;
import com.camara.quality_on_demand.api.model.SessionInfo;
import com.camara.quality_on_demand.api.model.StatusInfo;
import com.camara.repository.QosSessionRepository;
import inet.ipaddr.IPAddressString;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

  private static final int NUMBER_OF_UNMASKED_CHARS = 4;
  private static final int FLOW_ID_UNKNOWN = -1;
  private static final String FLOW_DESCRIPTION_TEMPLATE_IN = "permit in ip from %s to %s";
  private static final String FLOW_DESCRIPTION_TEMPLATE_OUT = "permit out ip from %s to %s";

  private static final String QOS_PROFILE_UNKNOWN_ERROR_MESSAGE = "QoS profile <%s> unknown or disabled";

  private final EventHubService eventHubService;
  private final NetworkService networkService;
  private final NetworkConfig networkConfig;
  private final QodConfig qodConfig;
  private final QosSessionRepository sessionRepository;
  private final SessionModelMapper sessionModelMapper;
  private final TokenService tokenService;
  private final ValidationService validationService;
  private final QosProfileService qosProfileService;

  /**
   * Creates a session and if the {@link QosStatus} is "AVAILABLE" then send an event directly to the webhook (if configured).
   *
   * @param sessionRequest       - the request for creating a session
   * @param showDeviceInResponse - {@code true}, if the device shell be shown in responses
   * @return {@link SessionInfo}
   */
  public SessionInfo createSession(@NotNull CreateSession sessionRequest, boolean showDeviceInResponse) {
    var qosSession = createSession(sessionRequest);
    qosSession.setShowDeviceInResponse(showDeviceInResponse);
    log.info("Save QoS session {}", sessionRequest);
    save(qosSession);

    SessionInfo sessionInfo = sessionModelMapper.map(qosSession);
    if (sessionInfo.getQosStatus() == QosStatus.AVAILABLE) {
      eventHubService.sendEvent(sessionInfo);
    }
    return sessionInfo;
  }

  /**
   * Creates & saves session in database.
   *
   * @param sessionRequest The requested session
   */
  private QosSession createSession(CreateSession sessionRequest) {
    SupportedQosProfiles supportedQosProfile = SupportedQosProfiles.getProfileFromString(sessionRequest.getQosProfile());
    final int flowId = getFlowId(supportedQosProfile);

    String applicationServerIpv4Addr = sessionRequest.getApplicationServer().getIpv4Address();
    String deviceIpv4Addr = sessionRequest.getDevice().getIpv4Address().getPublicAddress();
    PortsSpec applicationServerPorts = sessionRequest.getApplicationServerPorts();
    PortsSpec devicePorts = sessionRequest.getDevicePorts();

    /* Check if a session already exists for the requested device */
    checkExistingSessions(deviceIpv4Addr, applicationServerIpv4Addr, devicePorts, applicationServerPorts);

    /* Check if the requested profile is available */
    QosProfile qosProfile = qosProfileService.getQosProfile(supportedQosProfile.name());
    validationService.validateDurationWithQosProfile(sessionRequest.getDuration(), qosProfile);

    applicationServerIpv4Addr = appendPortsToIpv4(applicationServerPorts, applicationServerIpv4Addr);
    deviceIpv4Addr = appendPortsToIpv4(devicePorts, deviceIpv4Addr);

    FlowInfo flowInfo = createFlowInfo(deviceIpv4Addr, applicationServerIpv4Addr, flowId);

    String qosReference = getReference(supportedQosProfile);
    AsSessionWithQoSSubscription response = networkService.createQosSubscription(sessionRequest, flowInfo, qosReference);

    String subscriptionId = Util.extractSubscriptionId(response.getSelf());
    if (subscriptionId == null) {
      throw new QodApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No valid subscription ID was provided in NEF/SCEF response");
    }
    var qosSession = buildQosSession(sessionRequest);
    qosSession.setSubscriptionId(subscriptionId);
    return qosSession;
  }

  private static String appendPortsToIpv4(PortsSpec portsSpec, String ipv4Address) {
    if (!isPortsSpecNotDefined(portsSpec)) {
      /* Validate port ranges generally beside the check in checkExistingSessions */
      checkPortRange(portsSpec);
      /* AS port present. Append it to IP */
      ipv4Address += " " + convertPorts(portsSpec);
    }
    return ipv4Address;
  }

  /**
   * Returns a {@link SessionInfo} for a provided session-ID, which was created by the caller.
   *
   * @param sessionId the session-ID
   * @return {@link SessionInfo}
   */
  public SessionInfo getSessionInfoById(UUID sessionId) {
    QosSession foundQosSession = getSessionByIdForClient(sessionId);
    return sessionModelMapper.map(foundQosSession);
  }

  private QosSession getSessionByIdForClient(UUID sessionId) {
    String clientId = tokenService.retrieveClientId();
    return getSessionsByClientId(clientId).stream()
        .filter(session -> session.getSessionId().equals(sessionId.toString()))
        .findFirst()
        .orElseThrow(() -> new QodApiException(HttpStatus.NOT_FOUND, "The specified session does not exist"));
  }

  /**
   * Finds existing sessions by client-ID.
   *
   * @param clientId the requested client-ID.
   */
  public List<QosSession> getSessionsByClientId(String clientId) {
    return sessionRepository.findAllByClientId(clientId);
  }

  /**
   * Finds existing session by id.
   *
   * @param sessionId the requested session-id.
   */
  public QosSession getSessionById(String sessionId) {
    return sessionRepository.findBySessionId(sessionId)
        .orElseThrow(() -> new QodApiException(HttpStatus.NOT_FOUND, getSessionNotFoundMessage(sessionId)));
  }

  /**
   * Finds existing session for a device.
   *
   * @param device the device.
   * @throws QodApiException with 403, if the client-ID is not given
   */
  public List<SessionInfo> getSessionsByDevice(Device device) {
    String clientId = tokenService.retrieveClientId();
    if (StringUtils.isEmpty(clientId)) {
      return Collections.emptyList();
    }
    String publicIpv4Address = device.getIpv4Address().getPublicAddress();
    log.debug("Fetching sessions for clientId: {} and public IPv4: {}", clientId, publicIpv4Address);
    return getSessionsByPublicIpv4(publicIpv4Address).stream()
        .filter(qosSession -> qosSession.getClientId().equals(clientId))
        .map(sessionModelMapper::map)
        .toList();
  }

  /**
   * Deletes the session by its sessionId and notifies with the given {@link SessionInfo}.
   *
   * @param sessionId  the session ID
   * @param statusInfo the {@link StatusInfo}
   */
  public void deleteAndNotify(String sessionId, StatusInfo statusInfo) {
    var qosSession = deleteSessionById(sessionId);
    SessionInfo sessionInfo = sessionModelMapper.map(qosSession);
    sessionInfo.statusInfo(statusInfo);
    if (statusInfo == StatusInfo.DELETE_REQUESTED) {
      handleRequestedDelete(sessionInfo);
    } else {
      sessionInfo.setQosStatus(QosStatus.UNAVAILABLE);
      eventHubService.sendEvent(sessionInfo);
    }
  }

  /**
   * Extends the duration of a Quality of Service (QoS) session identified by the given session ID.
   *
   * @param sessionId          The unique identifier of the QoS session to be extended. Must not be null.
   * @param additionalDuration The additional duration (in seconds) by which the QoS session is to be extended. Must not be null.
   * @return A {@link SessionInfo} object representing the extended QoS session.
   */
  public SessionInfo extendQosSession(@NotNull UUID sessionId, @NotNull Integer additionalDuration) {
    // Retrieve QoS session and validate its extendability
    QosSession qosSession = getSessionByIdForClient(sessionId);
    validationService.isSessionExtendable(qosSession);

    log.info("Extending session <{}> with additional duration <{}>", sessionId, additionalDuration);

    // Calculate new duration and validate it
    int newDuration = calculateNewDuration(qosSession, additionalDuration);

    // Update session attributes
    qosSession.setDuration(newDuration);
    var newExpiresAt = OffsetDateTime.parse(qosSession.getStartedAt()).plusSeconds(newDuration);
    qosSession.setExpiresAt(newExpiresAt.toString());

    log.info("Updated QoS session <{}> with new duration <{}> and expiresAt <{}>.",
        sessionId, newDuration, qosSession.getExpiresAt());

    return sessionModelMapper.map(save(qosSession));
  }

  /**
   * Calculates the new duration for a session, ensuring it does not exceed the maximum allowed duration.
   *
   * @param qosSession         The current QoS session.
   * @param additionalDuration The additional duration requested.
   * @return The new validated duration for the session.
   * @throws QodApiException If the current or new duration exceeds the maximum allowed duration.
   */
  private int calculateNewDuration(QosSession qosSession, int additionalDuration) {
    int oldDuration = qosSession.getDuration();
    int newDuration = oldDuration + additionalDuration;

    log.debug("Current duration: {}, additional: {}, calculated new duration: {}", oldDuration, additionalDuration, newDuration);

    QosProfile qosProfile = getValidatedQosProfile(qosSession);
    long maxDuration = qosProfileService.retrieveDurationInSeconds(qosProfile.getMaxDuration());

    if (oldDuration == maxDuration) {
      throw new QodApiException(HttpStatus.BAD_REQUEST,
          String.format("Session is already at max duration for QoS profile <%s>", qosProfile.getName()),
          ErrorCode.OUT_OF_RANGE);
    }

    if (newDuration > maxDuration) {
      log.info("New duration exceeds max duration of {} for profile <{}>. Setting to max duration.",
          maxDuration, qosProfile.getName());
      newDuration = (int) maxDuration;
    }

    return newDuration;
  }

  /**
   * Retrieves and validates the QoS profile associated with the given session.
   *
   * @param qosSession The current QoS session.
   * @return The validated QoS profile.
   * @throws QodApiException If the QoS profile is unavailable or invalid.
   */
  private QosProfile getValidatedQosProfile(QosSession qosSession) {
    QosProfile qosProfile = qosProfileService.getQosProfile(qosSession.getQosProfile());
    validationService.validateQosProfileAvailability(qosProfile.getStatus());
    return qosProfile;
  }

  /**
   * Retrieves all Qos-Sessions which are expiring soon.
   *
   * @return list of almost expired {@link QosSession}
   */
  public List<QosSession> getExpiringQosSessions() {
    // Filter subscriptions based on their expiry time and deletion status
    return sessionRepository.findAll().stream()
        .filter(this::isExpiringSoon)
        .toList();
  }

  private boolean isExpiringSoon(QosSession qosSession) {
    // Get the current time in UTC
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

    // Calculate the time when a subscription is considered as expiring soon
    OffsetDateTime timeOfExpiration = now.plusSeconds(qodConfig.getQosExpirationTimeBeforeHandling());

    var expiresAt = OffsetDateTime.parse(qosSession.getExpiresAt());
    boolean isScheduledForDeletion = qosSession.isScheduledForDeletion();

    // Return true if the expiration date is before the calculated expiration time and the subscription is not scheduled for deletion
    if (expiresAt.isBefore(timeOfExpiration) && !isScheduledForDeletion) {
      return true;
    }

    // Return true if the subscription is scheduled for deletion and the expiration is in the past
    return isScheduledForDeletion && expiresAt.isBefore(now);
  }

  public QosSession save(QosSession qosSession) {
    return sessionRepository.save(qosSession);
  }

  private QosSession buildQosSession(CreateSession sessionRequest) {
    String clientId = tokenService.retrieveClientId();
    long defaultExpirationTimeInSeconds = qodConfig.getDefaultExpirationTimeInSeconds();
    var now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

    QosSession qosSession =
        QosSession.builder()
            .clientId(clientId)
            .sessionId(UUID.randomUUID().toString())
            .expiresAt(now.plusSeconds(defaultExpirationTimeInSeconds).format(ISO_DATE_TIME))
            .duration(sessionRequest.getDuration())
            .deviceIpv4addr(sessionRequest.getDevice().getIpv4Address().getPublicAddress())
            .device(sessionRequest.getDevice())
            .applicationServer(sessionRequest.getApplicationServer())
            .devicePorts(sessionRequest.getDevicePorts())
            .applicationServerPorts(sessionRequest.getApplicationServerPorts())
            .qosProfile(sessionRequest.getQosProfile())
            .sink(sessionRequest.getSink())
            .sinkCredential(sessionRequest.getSinkCredential())
            .build();

    if (networkConfig.isSupportedEventResourceAllocation()) {
      qosSession.setQosStatus(QosStatus.REQUESTED);
    } else {
      int duration = sessionRequest.getDuration();
      var expiresAt = now.plusSeconds(duration).format(ISO_DATE_TIME);
      qosSession.setQosStatus(QosStatus.AVAILABLE);
      qosSession.setStartedAt(now.format(ISO_DATE_TIME));
      qosSession.setExpiresAt(expiresAt);
    }

    return qosSession;
  }

  private List<QosSession> getSessionsByPublicIpv4(String publicIpv4Address) {
    return sessionRepository
        .findByDeviceIpv4addr(publicIpv4Address)
        .stream()
        .toList();
  }

  public Optional<QosSession> findBySubscriptionId(String subscriptionId) {
    return sessionRepository.findBySubscriptionId(subscriptionId);
  }

  private void handleRequestedDelete(SessionInfo sessionInfo) {
    if (sessionInfo.getQosStatus() == QosStatus.AVAILABLE) {
      sessionInfo.setQosStatus(QosStatus.UNAVAILABLE);
      eventHubService.sendEvent(sessionInfo);
    }
  }

  /**
   * Finds & removes session from the database.
   */
  private QosSession deleteSessionById(String sessionId) {
    QosSession qosSession = getSessionById(sessionId);

    log.info("Delete QoS session for sessionId <{}>", sessionId);
    sessionRepository.deleteBySessionId(sessionId);

    if (qosSession.getSubscriptionId() != null) {
      networkService.deleteNetworkSubscriptionById(sessionId);
    } else {
      log.info("A corresponding network-subscription for this session does not exist - no network subscription-deletion performed");
    }
    return qosSession;
  }

  /**
   * Generates a formatted error message for a session-not-found-scenario.
   *
   * @param sessionId The ID of the session that was not found.
   * @return A formatted error message indicating that the QoD session was not found for the provided session ID.
   */
  private static String getSessionNotFoundMessage(String sessionId) {
    return "QoD session not found for session ID: " + sessionId;
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
   * Looks for existing sessions with the same ipv4 address, if existing network or ports intersect with the given parameters.
   *
   * @param deviceIpv4             the user equipment address
   * @param applicationServerIpv4  the application server address
   * @param devicePorts            the user equipment ports
   * @param applicationServerPorts the application server ports
   */
  private void checkExistingSessions(String deviceIpv4,
      String applicationServerIpv4,
      PortsSpec devicePorts,
      PortsSpec applicationServerPorts) {

    List<QosSession> qosSessions = getSessionsByPublicIpv4(deviceIpv4);

    Optional<QosSession> sessionOptional = qosSessions.stream()
        .filter(qosSession -> checkNetworkIntersection(applicationServerIpv4, qosSession.getApplicationServer().getIpv4Address()))
        .filter(qosSession -> checkPortIntersection(
            isPortsSpecNotDefined(devicePorts)
                ? new PortsSpec().ranges(Collections.singletonList(new PortsSpecRangesInner().from(0).to(65535)))
                : devicePorts, (isPortsSpecNotDefined(qosSession.getDevicePorts())) ? new PortsSpec().ranges(
                Collections.singletonList(new PortsSpecRangesInner().from(0).to(65535))) : qosSession.getDevicePorts()))
        .filter(qosSession -> checkPortIntersection(
            isPortsSpecNotDefined(applicationServerPorts)
                ? new PortsSpec().ranges(Collections.singletonList(new PortsSpecRangesInner().from(0).to(65535)))
                : applicationServerPorts, (isPortsSpecNotDefined(qosSession.getApplicationServerPorts())) ? new PortsSpec().ranges(
                Collections.singletonList(new PortsSpecRangesInner().from(0).to(65535))) : qosSession.getApplicationServerPorts()))
        .findFirst();
    if (sessionOptional.isPresent()) {
      QosSession session = sessionOptional.get();
      String sessionId = qodConfig.isQosMaskSensibleData() ? maskString(session.getSessionId()) : session.getSessionId();
      throw new QodApiException(HttpStatus.CONFLICT, "Found session " + sessionId + " already active until " + session.getExpiresAt());
    }
  }

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
   * Checks if ports range is ordered from lower to higher and are in 0-65535.
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
  private static boolean checkPortIntersection(PortsSpec newSessionPorts, PortsSpec existingPorts) {
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

}
