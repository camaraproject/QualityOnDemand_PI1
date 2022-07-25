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
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.client.BookkeeperClient;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Service, that supports the implementations of the requests to the bookkeeper API. */
@Service
public class BookkeeperServiceImpl implements BookkeeperService {
  private static final Logger log = LoggerFactory.getLogger(BookkeeperService.class);

  private final BookkeeperClient bookkeeperClient;
  SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

  public BookkeeperServiceImpl(BookkeeperClient bookkeeperClient) {
    this.bookkeeperClient = bookkeeperClient;
  }

  @Override
  public Boolean checkBookingAvailability(
      UUID uuid, Date startsAt, Date expiresAt, CreateSession session) {
    // fill related party information
    AvailabilityRequest.RelatedParty relatedParty =
        AvailabilityRequest.RelatedParty.builder()
            .id("000000000331")
            .role("Customer")
            .name("HNCE GmbH")
            .build();

    // fill information about requested service
    List<ServiceCharacteristic> serviceCharacteristics =
        createServiceCharacteristics(session.getQos());
    AvailabilityRequest.Service service =
        AvailabilityRequest.Service.builder()
            .serviceSpecification(new ServiceSpecification())
            .serviceCharacteristic(serviceCharacteristics)
            .build();
    List<AvailabilityRequest.ServiceQualificationItem> serviceQualificationItems =
        new ArrayList<>();
    serviceQualificationItems.add(
        AvailabilityRequest.ServiceQualificationItem.builder()
            .id("1")
            .requestedStartDate(simpleDateFormat.format(startsAt))
            .requestedEndDate(simpleDateFormat.format(expiresAt))
            .service(service)
            .build());

    // create requestBody for bookkeeper API
    AvailabilityRequest body =
        AvailabilityRequest.builder()
            .description("Check booking availability for session with id " + uuid)
            .expectedQualificationDate(
                simpleDateFormat.format(
                    Date.from(Instant.ofEpochSecond(Instant.now().getEpochSecond() + 10))))
            .externalId(uuid.toString())
            .instantSyncQualification(true)
            .provideAlternative(false)
            .provideUnavailabilityReason(true)
            .relatedParty(relatedParty)
            .serviceQualificationItem(serviceQualificationItems)
            .build();

    log.debug("Send availability request to bookeeper API: " + body);

    // send post request to bookkeeper
    Map responseBody = bookkeeperClient.checkServiceQualification(body);
    log.debug("Availability response from bookeeper API: " + responseBody);

    // return if requested booking is available (true = available, false = unavailable)
    return responseBody.get("qualificationResult").equals("qualified");
  }

  @Override
  public UUID createBooking(UUID uuid, Date startsAt, Date expiresAt, CreateSession session) {
    // fill information about requested service
    List<ServiceCharacteristic> serviceCharacteristics =
        createServiceCharacteristics(session.getQos());

    // create requestBody for bookkeeper API
    BookingRequest body =
        BookingRequest.builder()
            .description("Create booking for session with id " + uuid)
            .state(AvailabilityState.active)
            .startDate(simpleDateFormat.format(startsAt))
            .endDate(simpleDateFormat.format(expiresAt))
            .serviceSpecification(new ServiceSpecification())
            .serviceCharacteristic(serviceCharacteristics)
            .build();

    log.debug("Send booking request to bookeeper API: " + body);

    // send post request to bookkeeper
    Map responseBody = bookkeeperClient.createBooking(body);
    log.debug("Booking response from bookeeper API: " + responseBody);

    // return if booking is active (true = active, false = inactive)
    return responseBody.get("state").equals("active")
        ? UUID.fromString(responseBody.get("id").toString())
        : null;
  }

  @Override
  public Boolean deleteBooking(UUID bookkeeperId) {
    log.debug("Send booking deletion request to bookeeper API with bookkeeper id: " + bookkeeperId);
    // return if booking was deleted successfully (true = deleted, false = not deleted)
    return bookkeeperClient.deleteBooking(bookkeeperId).getStatusCode() == HttpStatus.NO_CONTENT;
  }

  private List<ServiceCharacteristic> createServiceCharacteristics(QosProfile qos) {
    List<ServiceCharacteristic> serviceCharacteristics = new ArrayList<>();
    if (qos == QosProfile.LOW_LATENCY) {
      serviceCharacteristics.add(
          ServiceCharacteristic.builder()
              .name(Name.QualityProfile)
              .value(Value.low_latency)
              .build());
    } else {
      Value qosSize = Value.s;
      switch (qos) {
        case THROUGHPUT_M:
          qosSize = Value.m;
          break;
        case THROUGHPUT_L:
          qosSize = Value.l;
          break;
      }
      serviceCharacteristics.add(
          ServiceCharacteristic.builder().name(Name.QualityProfile).value(Value.embb_HNCE).build());
      serviceCharacteristics.add(
          ServiceCharacteristic.builder().name(Name.DLThroughput).value(qosSize).build());
      serviceCharacteristics.add(
          ServiceCharacteristic.builder().name(Name.ULThroughput).value(qosSize).build());
    }

    return serviceCharacteristics;
  }

  /**
   * This is the Availability Request resource for the bookkeeper API.
   */
  @Builder
  @Getter
  public static class AvailabilityRequest {
    private String description;
    private String expectedQualificationDate;
    private String externalId;
    private Boolean instantSyncQualification;
    private Boolean provideAlternative;
    private Boolean provideUnavailabilityReason;
    private RelatedParty relatedParty;
    private List<ServiceQualificationItem> serviceQualificationItem;

    @Builder
    @Getter
    static class RelatedParty {
      private String id;
      private String role;
      private String name;
    }

    @Builder
    @Getter
    static class ServiceQualificationItem {
      private String id;
      private String requestedStartDate;
      private String requestedEndDate;
      private Service service;
    }

    @Builder
    @Getter
    static class Service {
      private ServiceSpecification serviceSpecification;
      private List<Place> place;
      private List<ServiceCharacteristic> serviceCharacteristic;
    }
  }

  /**
   * This is the Booking Request resource for the bookkeeper API.
   */
  @Builder
  @Getter
  public static class BookingRequest {
    private String name;
    private String description;
    private AvailabilityState state;
    private String startDate;
    private String endDate;
    private ServiceSpecification serviceSpecification;
    private List<Place> place;
    private List<ServiceCharacteristic> serviceCharacteristic;
  }

  enum AvailabilityState {
    active,
    inactive
  }

  @Getter
  static class ServiceSpecification {
    private final String id = "bookkeeper-HNCE-service";
  }

  @Builder
  @Getter
  static class Place {
    private String id;
    private String role;
    private String name;
    private String referredType;
  }

  @Builder
  @Getter
  static class ServiceCharacteristic {
    private Name name;
    private Value value;
  }

  enum Name {
    QualityProfile,
    DLThroughput,
    ULThroughput
  }

  enum ValueType {
    String
  }

  enum Value {
    embb_HNCE,
    low_latency,
    s,
    m,
    l
  }
}
