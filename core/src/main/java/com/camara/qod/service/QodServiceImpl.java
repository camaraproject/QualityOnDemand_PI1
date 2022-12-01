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

package com.camara.qod.service;

import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.Message;
import com.camara.qod.api.model.Notification;
import com.camara.qod.api.model.PortsSpec;
import com.camara.qod.api.model.PortsSpecRanges;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.SessionEvent;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.api.notifications.SessionNotificationsCallbackApi;
import com.camara.qod.commons.Util;
import com.camara.qod.config.QodConfig;
import com.camara.qod.config.ScefConfig;
import com.camara.qod.exception.ErrorCode;
import com.camara.qod.exception.SessionApiException;
import com.camara.qod.feign.AvailabilityServiceClient;
import com.camara.qod.mapping.SessionModelMapper;
import com.camara.qod.model.AvailabilityRequest;
import com.camara.qod.model.QosSession;
import com.camara.scef.api.ApiClient;
import com.camara.scef.api.AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi;
import com.camara.scef.api.AsSessionWithQoSApiSubscriptionLevelPostOperationApi;
import com.camara.scef.api.model.AsSessionWithQoSSubscription;
import com.camara.scef.api.model.FlowInfo;
import com.camara.scef.api.model.UserPlaneEvent;
import feign.FeignException;
import inet.ipaddr.IPAddressString;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Service, that supports the implementations of the methods of the sessions' path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QodServiceImpl implements QodService {

  private static final int FLOW_ID_UNKNOWN = -1;
  private static final String FLOW_DESCRIPTION_TEMPLATE_IN = "permit from %s to %s";
  private static final String FLOW_DESCRIPTION_TEMPLATE_OUT = "permit from %s to %s";
  private static final String[] PRIVATE_NETWORKS = {"10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"};

  /**
   * SCEF QoS API, see http://www.3gpp.org/ftp/Specs/archive/29_series/29.122/
   */
  private final ScefConfig scefConfig;
  private final QodConfig qodConfig;
  private final SessionModelMapper sessionModelMapper;
  private final StorageService storage;
  private final AsSessionWithQoSApiSubscriptionLevelPostOperationApi postApi;
  private final AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi deleteApi;
  private final SessionNotificationsCallbackApi notificationsCallbackApi;
  private final AvailabilityServiceClient avsClient;
  private final ApiClient apiClient;

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
   * Masks the first four characters given string.
   *
   * @see QodServiceImpl#maskString(String, int)
   */
  private static String maskString(String unmaskedString) {
    return maskString(unmaskedString, 4);
  }

  /**
   * Masks a string.
   *
   * @param unmaskedString        string to mask
   * @param numberOfUnmaskedChars defines how many characters won't be masked
   * @return transformed unmaskedString so that each character from its beginning is replaced with X, only last numberOfUnmaskedChars
   *     characters won't be changed.
   */
  private static String maskString(String unmaskedString, int numberOfUnmaskedChars) {
    int indexOfMaskDelimiter = unmaskedString.length() - numberOfUnmaskedChars;
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
   * @param ports {@link PortsSpecRanges}
   */
  private static void checkPortRange(PortsSpec ports) {
    if (ports == null) {
      return;
    }
    if (ports.getRanges() == null) {
      return;
    }
    for (var portsSpecRanges : ports.getRanges()) {
      if (portsSpecRanges.getFrom() > portsSpecRanges.getTo()) {
        throw new SessionApiException(HttpStatus.BAD_REQUEST,
            "Ports specification not valid, given range: from " + portsSpecRanges.getFrom() + ", to " + portsSpecRanges.getTo(),
            ErrorCode.VALIDATION_FAILED);
      }
    }
    if (ports.getPorts() != null) {
      if (ports.getPorts().stream().max(Integer::compareTo).get() > 65535 || ports.getPorts().stream().min(Integer::compareTo).get() < 0) {
        throw new SessionApiException(HttpStatus.BAD_REQUEST, "Ports ranges are not valid (0-65535)",
            ErrorCode.VALIDATION_FAILED);
      }
    }
  }

  /**
   * Check if the ports of a new requested session are already defined in an active Session.
   *
   * @param newSessionPorts Ports of the new requested Session
   * @param existingPorts   Ports of active sessions
   * @return true for ports already in use, false for ports not in use
   */
  public static boolean checkPortIntersection(PortsSpec newSessionPorts, PortsSpec existingPorts) {
    // ports list comparison
    if (newSessionPorts.getPorts() != null && existingPorts.getPorts() != null) {
      if (newSessionPorts.getPorts().stream().anyMatch(existingPorts.getPorts()::contains)) {
        return true;
      }
    }

    // ports list & ports range comparison
    if (newSessionPorts.getPorts() != null && existingPorts.getRanges() != null) {
      for (var range : existingPorts.getRanges()) {
        if (newSessionPorts.getPorts().stream().anyMatch(port -> range.getFrom() <= port && range.getTo() >= port)) {
          return true;
        }
      }
    }
    if (existingPorts.getPorts() != null && newSessionPorts.getRanges() != null) {
      for (var range : newSessionPorts.getRanges()) {
        if (existingPorts.getPorts().stream().anyMatch(port -> range.getFrom() <= port && range.getTo() >= port)) {
          return true;
        }
      }
    }

    // ports range comparison
    if (newSessionPorts.getRanges() != null && existingPorts.getRanges() != null) {
      for (var range : existingPorts.getRanges()) {
        if (newSessionPorts.getRanges().stream().anyMatch(port -> !(port.getTo() < range.getFrom()) && !(port.getFrom() > range.getTo()))) {
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
  @Override
  public SessionInfo createSession(@NotNull CreateSession session) {
    QosProfile qosProfile = session.getQos();
    final int flowId = getFlowId(qosProfile);
    List<Message> messages = new ArrayList<>();

    // if multiple ueId.Ipv4Addr are not allowed and specified ueId.Ipv4Addr is a network segment, return error
    if (!qodConfig.getQosAllowMultipleUeAddr() && session.getUeId().getIpv4addr().matches(QodConfig.NETWORK_SEGMENT_REGEX)) {
      throw new SessionApiException(HttpStatus.BAD_REQUEST,
          "A network segment for UeIdIpv4Addr is not allowed in the current configuration: " + session.getUeId().getIpv4addr()
              + " is not allowed, but " + session.getUeId().getIpv4addr().substring(0, session.getUeId().getIpv4addr().indexOf("/"))
              + " is allowed.", ErrorCode.NOT_ALLOWED);
    }

    // Validate if ipv4 address is given
    if (session.getUeId().getIpv4addr() == null) {
      throw new SessionApiException(HttpStatus.BAD_REQUEST, "Validation failed for parameter 'ueId.ipv4addr'",
          ErrorCode.PARAMETER_MISSING);
    }
    if (session.getAsId().getIpv4addr() == null) {
      throw new SessionApiException(HttpStatus.BAD_REQUEST, "Validation failed for parameter 'asId.ipv4addr'",
          ErrorCode.PARAMETER_MISSING);
    }

    // Check if already exists
    Optional<QosSession> actual = checkExistingSessions(session.getUeId().getIpv4addr(), session.getAsId().getIpv4addr(),
        session.getUePorts(), session.getAsPorts());
    if (actual.isPresent()) {
      QosSession s = actual.get();
      Instant expirationTime = Instant.ofEpochSecond(s.getExpiresAt());
      String sessionId = qodConfig.getQosMaskSensibleData() ? maskString(s.getId().toString()) : s.getId().toString();
      throw new SessionApiException(HttpStatus.CONFLICT, "Found session " + sessionId + " already active until " + expirationTime);
    }

    // Check if asId.Ipv4Addr could be in private network
    for (String privateNetwork : PRIVATE_NETWORKS) {
      IPAddressString pn = new IPAddressString(privateNetwork);
      if (pn.contains(new IPAddressString(session.getAsId().getIpv4addr()))) {
        Message warning = new Message();
        String description = String.format("AS address range is in private network (%s). Some features may not work properly.",
            privateNetwork);
        warning.setSeverity(Message.SeverityEnum.WARNING);
        warning.setDescription(description);
        messages.add(warning);
        log.warn(description);
        break;
      }
    }

    String qosReference = getReference(qosProfile);

    String asAddr = session.getAsId().getIpv4addr();
    String ueAddr = session.getUeId().getIpv4addr();

    if (!isPortsSpecNotDefined(session.getAsPorts())) {
      // Validate port ranges generally beside the check in checkExistingSessions
      checkPortRange(session.getAsPorts());
      // AS port present. Append it to IP
      asAddr += " " + convertPorts(session.getAsPorts());
    }
    if (!isPortsSpecNotDefined(session.getUePorts())) {
      // Validate port ranges generally beside the check in checkExistingSessions
      checkPortRange(session.getUePorts());
      // UE port present. Append it to IP
      ueAddr += " " + convertPorts(session.getUePorts());
    }

    // UUID for session
    UUID uuid = UUID.randomUUID();

    // Time
    long now = Instant.now().getEpochSecond();
    int duration = session.getDuration();
    long expiresAt = now + duration;

    // check if requested booking is available and book it
    UUID bookkeeperId = qodConfig.getQosAvailabilityEnabled() ? createBooking(uuid, now, expiresAt, session) : null;

    FlowInfo flowInfo = createFlowInfo(ueAddr, asAddr, flowId);
    AsSessionWithQoSSubscription qosSubscription = createQosSubscription(session.getUeId().getIpv4addr(), flowInfo, qosReference,
        scefConfig.getSupportedFeatures());
    AsSessionWithQoSSubscription response = postApi.scsAsIdSubscriptionsPost(scefConfig.getScsAsId(), qosSubscription);
    String subscriptionId = Util.subscriptionId(response.getSelf());
    if (subscriptionId == null) {
      throw new SessionApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No valid subscription ID was provided in NEF/SCEF response");
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
  @Override
  public SessionInfo getSession(@NotNull UUID sessionId) {
    return storage.getSession(sessionId).map(sessionModelMapper::map)
        .orElseThrow(() -> new SessionApiException(HttpStatus.NOT_FOUND, "QoD session not found for session ID: " + sessionId));
  }

  /**
   * Finds & removes session from database.
   */
  @Override
  public SessionInfo deleteSession(@NotNull UUID sessionId) {
    QosSession qosSession = storage.getSession(sessionId)
        .orElseThrow(() -> new SessionApiException(HttpStatus.NOT_FOUND, "QoD session not found for session ID: " + sessionId));

    if (qodConfig.getQosAvailabilityEnabled()) {
      try {
        avsClient.deleteSession(qosSession.getBookkeeperId());
      } catch (FeignException e) {
        throw new SessionApiException(HttpStatus.SERVICE_UNAVAILABLE, "The availability service is currently not available");
      }
    }

    // delete the session
    storage.deleteSession(sessionId);

    if (qosSession.getSubscriptionId() != null) {
      try {
        deleteApi.scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(scefConfig.getScsAsId(), qosSession.getSubscriptionId());
      } catch (HttpStatusCodeException e) {
        throw new SessionApiException(HttpStatus.INTERNAL_SERVER_ERROR,
            "NEF/SCEF returned error " + e.getStatusCode() + " while deleting NEF/SCEF session with subscription ID: "
                + qosSession.getSubscriptionId());
      }
    }
    return sessionModelMapper.map(qosSession);
  }

  /**
   * Handles the QoS notification.
   *
   * @param subscriptionId the subscriptionid
   * @param event          the {@link UserPlaneEvent}
   * @return {@link CompletableFuture}
   */
  @Override
  @Async
  public CompletableFuture<Void> handleQosNotification(@NotBlank String subscriptionId, @NotNull UserPlaneEvent event) {
    Optional<QosSession> sessionOptional = storage.findBySubscriptionId(subscriptionId);
    if (sessionOptional.isPresent() && event.equals(UserPlaneEvent.SESSION_TERMINATION)) {
      QosSession session = sessionOptional.get();
      SessionInfo sessionInfo = deleteSession(session.getId());
      notifySession(sessionInfo, SessionEvent.SESSION_TERMINATED);

      if (session.getNotificationUri() != null && session.getNotificationAuthToken() != null) {
        com.camara.qod.api.notifications.ApiClient apiNotificationClient = new com.camara.qod.api.notifications.ApiClient().setBasePath(
            session.getNotificationUri().toString());
        apiNotificationClient.setApiKey(session.getNotificationAuthToken());
        notificationsCallbackApi.setApiClient(apiNotificationClient);
        notificationsCallbackApi.postNotification(new Notification().sessionId(session.getId()).event(SessionEvent.SESSION_TERMINATED));
      }
    }
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Takes care of expired sessions.
   *
   * @param qosSession the {@link SessionInfo}
   * @param event      {@link SessionEvent}
   * @return {@link CompletableFuture}
   */
  @Override
  @Async("taskScheduler")
  public CompletableFuture<Void> notifySession(@NotNull SessionInfo qosSession, @NotNull SessionEvent event) {
    if (qosSession.getNotificationUri() != null && qosSession.getNotificationAuthToken() != null) {
      com.camara.qod.api.notifications.ApiClient apiNotificationClient = new com.camara.qod.api.notifications.ApiClient().setBasePath(
          qosSession.getNotificationUri().toString());
      apiNotificationClient.setApiKey(qosSession.getNotificationAuthToken());
      notificationsCallbackApi.setApiClient(apiNotificationClient);
      notificationsCallbackApi.postNotification(new Notification().sessionId(qosSession.getId()).event(event));
    }
    return CompletableFuture.completedFuture(null);
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
    var qosProfile = session.getQos();
    var request = AvailabilityRequest.builder().uuid(uuid).startsAt(dateNow).expiresAt(dateExpire).qosProfile(qosProfile).build();

    try {
      avsClient.checkSession(request);
      var responseBody = avsClient.createSession(request).getBody();
      assert responseBody != null;
      return UUID.fromString(responseBody);
    } catch (FeignException e) {
      log.error("Problem by calling availability service: ", e);
      throw new SessionApiException(HttpStatus.SERVICE_UNAVAILABLE, "The availability service is currently not available");
    }
  }

  /**
   * Return the value for the corresponding {@link QosProfile}.
   *
   * @param profile the {@link QosProfile}
   * @return flow id from resources.
   */
  private int getFlowId(@NotNull QosProfile profile) {
    int flowId = switch (profile) {
      case E -> scefConfig.getFlowIdQosE();
      case S -> scefConfig.getFlowIdQosS();
      case M -> scefConfig.getFlowIdQosM();
      case L -> scefConfig.getFlowIdQosL();
    };

    if (flowId == FLOW_ID_UNKNOWN) {
      throw new SessionApiException(HttpStatus.BAD_REQUEST, "QoS profile unknown or disabled", ErrorCode.VALIDATION_FAILED);
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

    List<QosSession> qosSessions = storage.findByUeIpv4addr(ueAddr);

    return qosSessions.stream()
        .filter(qosSession -> checkNetworkIntersection(asAddr, qosSession.getAsId().getIpv4addr()))
        .filter(qosSession -> checkPortIntersection(
            isPortsSpecNotDefined(uePorts)
                ? new PortsSpec().ranges(Collections.singletonList(new PortsSpecRanges().from(0).to(65535)))
                : uePorts, (isPortsSpecNotDefined(qosSession.getUePorts())) ? new PortsSpec().ranges(
                Collections.singletonList(new PortsSpecRanges().from(0).to(65535))) : qosSession.getUePorts()))
        .filter(qosSession -> checkPortIntersection(
            isPortsSpecNotDefined(asPorts)
                ? new PortsSpec().ranges(Collections.singletonList(new PortsSpecRanges().from(0).to(65535)))
                : asPorts, (isPortsSpecNotDefined(qosSession.getAsPorts())) ? new PortsSpec().ranges(
                Collections.singletonList(new PortsSpecRanges().from(0).to(65535))) : qosSession.getAsPorts())).findFirst();
  }

  /**
   * Returns the reference to the corresponding {@link QosProfile}.
   *
   * @param profile the {@link QosProfile}
   * @return reference from resources.
   */
  private String getReference(QosProfile profile) {
    return switch (profile) {
      case E -> qodConfig.getQosReferenceQosE();
      case S -> qodConfig.getQosReferenceQosS();
      case M -> qodConfig.getQosReferenceQosM();
      case L -> qodConfig.getQosReferenceQosL();
    };
  }

  /**
   * Creates flow info.
   *
   * @param ueAddr UE ipv4 address + UE ports
   * @param asAddr AS ipv4 address + AS ports
   * @param flowId the flowId
   * @see QodServiceImpl#getFlowId
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
        .notificationDestination(scefConfig.getScefNotificationsDestination()).requestTestNotification(true)
        .supportedFeatures(supportedFeatures);
  }
}
