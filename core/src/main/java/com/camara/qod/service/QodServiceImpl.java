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

import com.camara.datatypes.model.QosSession;
import com.camara.qod.api.model.CheckQosAvailabilityResponse;
import com.camara.qod.api.model.CheckQosAvailabilityResponseQosProfiles;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.Message;
import com.camara.qod.api.model.Notification;
import com.camara.qod.api.model.Protocol;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.RenewSession;
import com.camara.qod.api.model.SessionEvent;
import com.camara.qod.api.model.SessionInfo;
import com.camara.qod.api.notifications.SessionNotificationsCallbackApi;
import com.camara.qod.commons.Util;
import com.camara.qod.config.QodConfig;
import com.camara.qod.config.ScefConfig;
import com.camara.qod.controller.SessionApiException;
import com.camara.qod.mapping.ModelMapper;
import com.camara.qod.plugin.storage.StorageInterface;
import com.camara.scef.api.ApiClient;
import com.camara.scef.api.AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi;
import com.camara.scef.api.AsSessionWithQoSApiSubscriptionLevelPostOperationApi;
import com.camara.scef.api.model.AsSessionWithQoSSubscription;
import com.camara.scef.api.model.FlowInfo;
import com.camara.scef.api.model.UserPlaneEvent;
import com.qod.model.BookkeeperCreateSession;
import com.qod.service.BookkeeperService;
import inet.ipaddr.IPAddressString;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Service, that supports the implementations of the methods of the sessions' path.
 */
@Service
@RequiredArgsConstructor
public class QodServiceImpl implements QodService {

  private static final Logger log = LoggerFactory.getLogger(QodServiceImpl.class);

  private static final int FLOW_ID_UNKNOWN = -1;
  private static final String FLOW_DESCRIPTION_TEMPLATE_IN = "permit in %s from %s to %s";
  private static final String FLOW_DESCRIPTION_TEMPLATE_OUT = "permit out %s from %s to %s";
  private static final String[] PRIVATE_NETWORKS = {
      "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"
  };

  /**
   * SCEF QoS API, see http://www.3gpp.org/ftp/Specs/archive/29_series/29.122/
   */
  private final ScefConfig scefConfig;

  private final QodConfig qodConfig;

  private final ModelMapper modelMapper;
  private final StorageInterface storage;
  private final AsSessionWithQoSApiSubscriptionLevelPostOperationApi postApi;
  private final AsSessionWithQoSApiSubscriptionLevelDeleteOperationApi deleteApi;
  private final SessionNotificationsCallbackApi notificationsCallbackApi;

  private final BookkeeperService bookkeeperService;

  private final ApiClient apiClient;

  @PostConstruct
  void setupApiClient() {
    postApi.setApiClient(apiClient);
    deleteApi.setApiClient(apiClient);
  }

  /**
   * Update certain attributes only included in UpdateSession class.
   */
  @Override
  public SessionInfo renewSession(UUID id, RenewSession renewSession) {
    QosSession qosSession =
        storage.getSession(id)
            .orElseThrow(
                () ->
                    new SessionApiException(
                        HttpStatus.NOT_FOUND, "QoD session not found for session ID: " + id));

    // Assert that the session is not planned to be deleted
    if (Instant.ofEpochSecond(
            qosSession.getExpiresAt() - qodConfig.getQosExpirationTimeBeforeHandling())
        .compareTo(Instant.now()) < 0) {
      log.debug("The session " + qosSession.getId()
          + "is planned to be executed, therefore it is not updated");
      throw new SessionApiException(HttpStatus.CONFLICT,
          "The session cannot be updated, it will soon expire.");
    }

    if (renewSession.getDuration() != null) {
      // Assert that startsAt + duration > now
      if (Instant.ofEpochSecond(renewSession.getDuration() + qosSession.getStartedAt())
          .compareTo(Instant.now()) <= 0) {
        log.debug(
            "The requested duration would cause the session "
                + qosSession.getId()
                + " to expire, therefore it is not updated.");
        throw new SessionApiException(
            HttpStatus.CONFLICT,
            "The requested duration would cause the session "
                + qosSession.getId()
                + " to expire, please delete the session if it is not needed anymore.");
      }

      Boolean updateResult;

      try {
        updateResult =
            bookkeeperService.changeBookingTime(
                id,
                Date.from(
                    Instant.ofEpochSecond(
                        qosSession.getStartedAt() + renewSession.getDuration())));
      } catch (Exception e) {
        throw new SessionApiException(
            HttpStatus.SERVICE_UNAVAILABLE, "The service is currently not available");
      }

      if (!updateResult) {
        throw new SessionApiException(
            HttpStatus.CONFLICT, "Requested QoS session is currently not available");
      }

      qosSession.setDuration(renewSession.getDuration());
      qosSession.setExpiresAt(qosSession.getStartedAt() + renewSession.getDuration());

      storage.saveSession(qosSession);
      storage.addExpiration(id, qosSession.getExpiresAt());
    }
    return modelMapper.map(qosSession);
  }

  @Override
  public SessionInfo createSession(@NotNull CreateSession session) {
    QosProfile qosProfile = session.getQos();
    final int flowId = getFlowId(qosProfile);
    List<Message> messages = new ArrayList<>();

    // if multiple ueAddr are not allowed and specified ueAddr is a network segment, return error
    if (!qodConfig.getQosAllowMultipleUeAddr()
        && session.getUeAddr().matches(QodConfig.NETWORK_SEGMENT_REGEX)) {
      throw new SessionApiException(
          HttpStatus.BAD_REQUEST,
          "A network segment for ueAddr is not allowed in the current configuration: "
              + session.getUeAddr()
              + " is not allowed, but "
              + session.getUeAddr().substring(0, session.getUeAddr().indexOf("/"))
              + " is allowed.");
    }

    // Check if already exists
    Optional<QosSession> actual =
        checkExistingSessions(
            session.getUeAddr(),
            session.getAsAddr(),
            session.getUePorts(),
            session.getAsPorts(),
            session.getProtocolIn(),
            session.getProtocolOut());
    if (actual.isPresent()) {
      QosSession s = actual.get();
      Instant expirationTime = Instant.ofEpochSecond(s.getExpiresAt());
      String sessionId =
          qodConfig.getQosMaskSensibleData()
              ? maskString(s.getId().toString())
              : s.getId().toString();
      throw new SessionApiException(
          HttpStatus.CONFLICT,
          "Found session " + sessionId + " already active until " + expirationTime);
    }

    // Check if asAddr is could be in private network
    for (String privateNetwork : PRIVATE_NETWORKS) {
      IPAddressString pn = new IPAddressString(privateNetwork);
      if (pn.contains(new IPAddressString(session.getAsAddr()))) {
        Message warning = new Message();
        String description = String.format(
            "AS address range is in private network (%s). Some features may not work properly.",
            privateNetwork);
        warning.setSeverity(Message.SeverityEnum.WARNING);
        warning.setDescription(description);
        messages.add(warning);
        log.warn(description);
        break;
      }
    }

    String qosReference = getReference(qosProfile);
    String protocolIn = getProtocol(session.getProtocolIn());
    String protocolOut = getProtocol(session.getProtocolOut());

    String asAddr = session.getAsAddr();
    String ueAddr = session.getUeAddr();

    if (session.getAsPorts() != null) {
      // Validate port ranges generally beside the check in checkExistingSessions
      checkPortRange(session.getAsPorts());
      // AS port present. Append it to IP
      asAddr += " " + session.getAsPorts();
    }
    if (session.getUePorts() != null) {
      // Validate port ranges generally beside the check in checkExistingSessions
      checkPortRange(session.getUePorts());
      // UE port present. Append it to IP
      ueAddr += " " + session.getUePorts();
    }

    // UUID for session
    UUID uuid = UUID.randomUUID();

    // Time
    long now = Instant.now().getEpochSecond();
    int duration = session.getDuration();
    long expiresAt = now + duration;

    // check if requested booking is available and book it
    UUID bookkeeperId =
        qodConfig.getQosBookkeeperEnabled() ? createBooking(uuid, now, expiresAt, session) : null;

    FlowInfo flowInfo = createFlowInfo(ueAddr, asAddr, protocolIn, protocolOut, flowId);
    AsSessionWithQoSSubscription qosSubscription =
        createQosSubscription(
            session.getUeAddr(), flowInfo, qosReference, scefConfig.getSupportedFeatures());
    AsSessionWithQoSSubscription response =
        postApi.scsAsIdSubscriptionsPost(scefConfig.getScsAsId(), qosSubscription);
    String subscriptionId = Util.subscriptionId(response.getSelf());
    if (subscriptionId == null) {
      throw new SessionApiException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "No valid subscription ID was provided in NEF/SCEF response");
    }

    log.info("Save QoS session " + session);

    QosSession qosSession = storage.saveSession(now, expiresAt, uuid, session, subscriptionId,
        bookkeeperId);
    // add an entry to a sorted set which contains the session id and the expiration time
    storage.addExpiration(uuid, expiresAt);

    SessionInfo ret = modelMapper.map(qosSession);

    // Messages are only present in response but not in repository
    ret.setMessages(messages);
    return ret;
  }

  @Override
  public SessionInfo getSession(@NotNull UUID sessionId) {
    return storage.getSession(sessionId)
        .map(modelMapper::map)
        .orElseThrow(
            () ->
                new SessionApiException(
                    HttpStatus.NOT_FOUND, "QoD session not found for session ID: " + sessionId));
  }

  @Override
  public SessionInfo deleteSession(@NotNull UUID sessionId) {
    QosSession qosSession = storage.getSession(sessionId)
        .orElseThrow(
            () ->
                new SessionApiException(
                    HttpStatus.NOT_FOUND,
                    "QoD session not found for session ID: " + sessionId));

    if (qodConfig.getQosBookkeeperEnabled()) {
      try {
        bookkeeperService.deleteBooking(qosSession.getBookkeeperId());
      } catch (Exception e) {
        throw new SessionApiException(HttpStatus.SERVICE_UNAVAILABLE,
            "The service is currently not available");
      }
    }

    // delete the session
    storage.deleteSession(sessionId);
    // delete the entry of the sorted set (id, expirationTime)
    storage.removeExpiration(sessionId);

    if (qosSession.getSubscriptionId() != null) {
      // TODO properly process NEF/SCEF error codes
      try {
        deleteApi.scsAsIdSubscriptionsSubscriptionIdDeleteWithHttpInfo(
            scefConfig.getScsAsId(), qosSession.getSubscriptionId());
      } catch (HttpStatusCodeException e) {
        throw new SessionApiException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "NEF/SCEF returned error "
                + e.getStatusCode()
                + " while deleting NEF/SCEF session with subscription ID: "
                + qosSession.getSubscriptionId());
      }
    }
    return modelMapper.map(qosSession);
  }

  @Override
  @Async
  public CompletableFuture<Void> handleQosNotification(
      @NotBlank String subscriptionId, @NotNull UserPlaneEvent event) {
    Optional<QosSession> sessionOptional = storage.findBySubscriptionId(subscriptionId);
    // TODO implement proper event model. For now only SESSION_TERMINATED event is supported
    if (sessionOptional.isPresent() && event.equals(UserPlaneEvent.SESSION_TERMINATION)) {
      QosSession session = sessionOptional.get();
      SessionInfo sessionInfo = deleteSession(session.getId());
      notifySession(sessionInfo, SessionEvent.SESSION_TERMINATED);

      if (session.getNotificationUri() != null && session.getNotificationAuthToken() != null) {
        com.camara.qod.api.notifications.ApiClient apiNotificationClient =
            new com.camara.qod.api.notifications.ApiClient()
                .setBasePath(session.getNotificationUri().toString());
        apiNotificationClient.setApiKey(session.getNotificationAuthToken());
        notificationsCallbackApi.setApiClient(apiNotificationClient);
        notificationsCallbackApi.postNotification(
            new Notification().sessionId(session.getId()).event(SessionEvent.SESSION_TERMINATED));
      }
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  @Async("taskScheduler") // @EnableScheduling and @EnableAsync both define a task executor, thus we
  // need to specify which one to use
  public CompletableFuture<Void> notifySession(@NotNull SessionInfo qosSession, @NotNull SessionEvent event) {
    if (qosSession.getNotificationUri() != null && qosSession.getNotificationAuthToken() != null) {
      com.camara.qod.api.notifications.ApiClient apiNotificationClient =
          new com.camara.qod.api.notifications.ApiClient()
              .setBasePath(qosSession.getNotificationUri().toString());
      apiNotificationClient.setApiKey(qosSession.getNotificationAuthToken());
      notificationsCallbackApi.setApiClient(apiNotificationClient);
      notificationsCallbackApi.postNotification(new Notification().sessionId(qosSession.getId()).event(event));
    }
    return CompletableFuture.completedFuture(null);
  }

  private UUID createBooking(UUID uuid, long now, long expiresAt, CreateSession session) {
    Boolean bookkeeperResult;

    // TEMP SOLUTION
    BookkeeperCreateSession bookkprSession = modelMapper.map(session);

    try {
      bookkeeperResult =
          this.bookkeeperService.checkBookingAvailability(
              uuid,
              Date.from(Instant.ofEpochSecond(now)),
              Date.from(Instant.ofEpochSecond(expiresAt)),
              bookkprSession);
    } catch (Exception e) {
      throw new SessionApiException(
          HttpStatus.SERVICE_UNAVAILABLE, "The service is currently not available");
    }

    if (!bookkeeperResult) {
      throw new SessionApiException(
          HttpStatus.CONFLICT, "Requested QoS session is currently not available");
    }

    return this.bookkeeperService.createBooking(
        uuid,
        Date.from(Instant.ofEpochSecond(now)),
        Date.from(Instant.ofEpochSecond(expiresAt)),
        bookkprSession);
  }

  private int getFlowId(@NotNull QosProfile profile) {
    int flowId = switch (profile) {
      case LOW_LATENCY -> scefConfig.getFlowIdLowLatency();
      case THROUGHPUT_S -> scefConfig.getFlowIdThroughputS();
      case THROUGHPUT_M -> scefConfig.getFlowIdThroughputM();
      case THROUGHPUT_L -> scefConfig.getFlowIdThroughputL();
    };

    if (flowId == FLOW_ID_UNKNOWN) {
      throw new SessionApiException(HttpStatus.BAD_REQUEST, "QoS profile unknown or disabled");
    }
    return flowId;
  }

  private Optional<QosSession> checkExistingSessions(
      String ueAddr,
      String asAddr,
      String uePorts,
      String asPorts,
      Protocol inProtocol,
      Protocol outProtocol) {
    List<QosSession> qosSessions = storage.findByUeAddr(ueAddr);

    return qosSessions.stream()
        .filter(qosSession -> checkNetworkIntersection(asAddr, qosSession.getAsAddr()))
        .filter(
            qosSession ->
                checkPortIntersection(
                    uePorts == null ? "0-65535" : uePorts,
                    qosSession.getUePorts() == null ? "0-65535" : qosSession.getUePorts()))
        .filter(
            qosSession ->
                checkPortIntersection(
                    asPorts == null ? "0-65535" : asPorts,
                    qosSession.getAsPorts() == null ? "0-65535" : qosSession.getAsPorts()))
        .filter(qosSession -> checkProtocolIntersection(inProtocol, qosSession.getProtocolIn()))
        .filter(qosSession -> checkProtocolIntersection(outProtocol, qosSession.getProtocolOut()))
        .findFirst();
  }

  private String getReference(QosProfile profile) {
    return switch (profile) {
      case LOW_LATENCY -> qodConfig.getQosReferenceLowLatency();
      case THROUGHPUT_S -> qodConfig.getQosReferenceThroughputS();
      case THROUGHPUT_M -> qodConfig.getQosReferenceThroughputM();
      case THROUGHPUT_L -> qodConfig.getQosReferenceThroughputL();
    };
  }

  private String getProtocol(Protocol protocol) {
    return switch (protocol) {
      case ANY -> "ip";
      case TCP -> "6";
      case UDP -> "17";
    };
  }

  private FlowInfo createFlowInfo(
      String ueAddr, String asAddr, String protocolIn, String protocolOut, Integer flowId) {
    return new FlowInfo()
        .flowId(flowId)
        .addFlowDescriptionsItem(
            String.format(FLOW_DESCRIPTION_TEMPLATE_IN, protocolIn, ueAddr, asAddr))
        .addFlowDescriptionsItem(
            String.format(FLOW_DESCRIPTION_TEMPLATE_OUT, protocolOut, asAddr, ueAddr));
  }

  private AsSessionWithQoSSubscription createQosSubscription(
      String ueAddr, FlowInfo flowInfo, String qosReference, String supportedFeatures) {
    return new AsSessionWithQoSSubscription()
        .ueIpv4Addr(ueAddr)
        .flowInfo(List.of(flowInfo))
        .qosReference(qosReference)
        .notificationDestination(scefConfig.getScefNotificationsDestination())
        .requestTestNotification(true)
        .supportedFeatures(supportedFeatures);
  }

  private String maskString(String unmaskedString) {
    return maskString(unmaskedString, 4);
  }

  private String maskString(String unmaskedString, int numberOfUnmaskedChars) {
    int indexOfMaskDelimiter = unmaskedString.length() - numberOfUnmaskedChars;
    return unmaskedString.substring(0, indexOfMaskDelimiter).replaceAll("[^-]", "X")
        + unmaskedString.substring(indexOfMaskDelimiter);
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
   * Check if the ports in range ordered from lower to higher.
   *
   * @param ports Ports to validate
   */
  private static void checkPortRange(String ports) {
    String[] portsArr = ports.split(",");
    for (String port : portsArr) {
      int[] portRange = parsePortsToPortRange(port);
      if (portRange[0] > portRange[1]) {
        throw new SessionApiException(HttpStatus.BAD_REQUEST, "Ports range not valid " + port);
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
  private static boolean checkPortIntersection(String newSessionPorts, String existingPorts) {
    String[] newSession = newSessionPorts.split(",");
    String[] existing = existingPorts.split(",");
    for (String newSessionPort : newSession) {
      int[] newPortRange = parsePortsToPortRange(newSessionPort);
      if (newPortRange[0] > newPortRange[1]) {
        throw new SessionApiException(
            HttpStatus.BAD_REQUEST, "Ports range not valid " + newSessionPort);
      }
      for (String existingPort : existing) {
        int[] existingPortRange = parsePortsToPortRange(existingPort);
        if (newPortRange[0] <= existingPortRange[1] && existingPortRange[0] <= newPortRange[1]) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Parse the ports string to integer ranges.
   *
   * @param ports (e.g. "5000-5600"; "1234")
   * @return integer port range (e.g. [5000, 5600]; [1234, 1234])
   */
  private static int[] parsePortsToPortRange(String ports) {
    if (!ports.contains("-")) {
      ports = ports + "-" + ports; // convert single port to port range
    }
    return Arrays.stream(ports.split("-")).mapToInt(Integer::parseInt).toArray();
  }

  /**
   * Check if the given protocols intersect.
   *
   * @param protocol1 protocol name (e.g. UDP/TCP/ANY)
   * @param protocol2 protocol name (e.g. UDP/TCP/ANY)
   * @return true for intersecting protocols, false otherwise
   */
  private static boolean checkProtocolIntersection(Protocol protocol1, Protocol protocol2) {
    if (protocol1 == Protocol.ANY || protocol2 == Protocol.ANY) {
      return true;
    } else {
      return protocol1.equals(protocol2);
    }
  }

  /**
   * Execute the check for QoS availability.
   *
   * @param ueId UE identifier
   * @return Http response with a list of available QoS profiles. Only available on the Throughput-API.
   */
  public CheckQosAvailabilityResponse checkQosAvailability(@NotNull String ueId) {
    if (scefConfig.getFlowIdThroughputS() == FLOW_ID_UNKNOWN) {
      throw new SessionApiException(HttpStatus.NOT_FOUND, "Resource not found");
    }
    // toDo: Change the mock implementation to the real implementation
    return new CheckQosAvailabilityResponse()
        .addQosProfilesItem(
            new CheckQosAvailabilityResponseQosProfiles().qos(QosProfile.THROUGHPUT_S))
        .addQosProfilesItem(
            new CheckQosAvailabilityResponseQosProfiles().qos(QosProfile.THROUGHPUT_M));
  }
}
