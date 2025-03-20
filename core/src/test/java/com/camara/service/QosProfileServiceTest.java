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

import static com.camara.util.QosProfilesTestData.getQosProfilesEntityTestData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.camara.exception.QodApiException;
import com.camara.model.SupportedQosProfiles;
import com.camara.qos_profiles.api.model.QosProfile;
import com.camara.qos_profiles.api.model.QosProfileStatusEnum;
import com.camara.repository.QosProfileRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class
})
@ActiveProfiles("test")
class QosProfileServiceTest {

  @MockitoBean
  private QosProfileRepository qosProfileRepository;

  @Autowired
  private QosProfileService qosProfileService;

  @MockitoBean
  private ExpiredSessionMonitor expiredSessionMonitor;

  @Test
  void testGetQosProfiles_getAll_ok() {
    when(qosProfileRepository.findAll()).thenReturn(getQosProfilesEntityTestData());
    List<QosProfile> qosProfiles = qosProfileService.getQosProfiles(null, null);
    assertEquals(4, qosProfiles.size());
    assertEquals("QOS_E", qosProfiles.getFirst().getName());
    assertEquals(QosProfileStatusEnum.ACTIVE, qosProfiles.getFirst().getStatus());
  }

  @Test
  void testGetQosProfiles_ByNameAndStatus_ok() {
    String qosProfileE = SupportedQosProfiles.QOS_E.name();
    when(qosProfileRepository.findAllByNameAndStatus(any(), any())).thenReturn(
        getQosProfilesEntityTestData().stream()
            .filter(qosProfile -> qosProfile.getName().equals(qosProfileE))
            .toList());
    List<QosProfile> qosProfiles = qosProfileService.getQosProfiles(qosProfileE, QosProfileStatusEnum.ACTIVE);
    assertEquals(1, qosProfiles.size());
    assertEquals(qosProfileE, qosProfiles.getFirst().getName());
    assertEquals(QosProfileStatusEnum.ACTIVE, qosProfiles.getFirst().getStatus());
  }

  @Test
  void testGetQosProfiles_ByStatusActive_ok() {
    when(qosProfileRepository.findAllByStatus(any())).thenReturn(
        getQosProfilesEntityTestData().stream()
            .filter(qosProfile -> qosProfile.getStatus().equals(QosProfileStatusEnum.ACTIVE))
            .toList());
    List<QosProfile> qosProfiles = qosProfileService.getQosProfiles(null, QosProfileStatusEnum.ACTIVE);
    assertEquals(4, qosProfiles.size()); // Assuming all test data profiles have ACTIVE status
  }

  @Test
  void testGetQosProfiles_ByName_ok() {
    String qosProfileE = SupportedQosProfiles.QOS_E.name();
    when(qosProfileRepository.findByName(any())).thenReturn(
        getQosProfilesEntityTestData().stream()
            .filter(qosProfile -> qosProfile.getName().equals(qosProfileE))
            .findFirst());
    List<QosProfile> qosProfiles = qosProfileService.getQosProfiles(qosProfileE, null);
    assertEquals(1, qosProfiles.size());
    assertEquals(qosProfileE, qosProfiles.getFirst().getName());
  }

  @Test
  void testGetQosProfiles_ByStatus_notFound_404() {
    QodApiException exception = assertThrows(QodApiException.class,
        () -> qosProfileService.getQosProfiles(null, QosProfileStatusEnum.DEPRECATED));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    assertEquals("No QoS Profiles found", exception.getMessage());
  }

  @Test
  void testGetQosProfiles_ByName_notFound_404() {
    QodApiException exception = assertThrows(QodApiException.class, () -> qosProfileService.getQosProfiles("NonExistent", null));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    assertEquals("QosProfile Id does not exist", exception.getMessage());
  }

  @Test
  void testGetQosProfile_ok() {
    String qosProfileE = SupportedQosProfiles.QOS_E.name();
    when(qosProfileRepository.findByName(any())).thenReturn(
        getQosProfilesEntityTestData().stream()
            .filter(qosProfile -> qosProfile.getName().equals(qosProfileE))
            .findFirst());
    QosProfile qosProfile = qosProfileService.getQosProfile(qosProfileE);
    assertEquals(qosProfileE, qosProfile.getName());
  }

  @Test
  void testGetQosProfile_notFound_404() {
    QodApiException exception = assertThrows(QodApiException.class, () -> qosProfileService.getQosProfile("NonExistent"));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    assertEquals("QosProfile Id does not exist", exception.getMessage());
  }
}
