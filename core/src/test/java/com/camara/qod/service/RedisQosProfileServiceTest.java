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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.camara.qod.api.model.QosProfile;
import com.camara.qod.api.model.QosProfileStatusEnum;
import com.camara.qod.exception.QodApiException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import redis.embedded.RedisServer;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class
})
@ActiveProfiles(profiles = "test")
class RedisQosProfileServiceTest {

  private static RedisServer redisServer = null;

  @BeforeAll
  public static void setUp() throws IOException {
    redisServer = new RedisServer(6370);
    if (redisServer.isActive()) {
      redisServer.stop();
    }
    redisServer.start();
  }

  @AfterAll
  public static void tearDown() throws IOException {
    redisServer.stop();
  }

  @Autowired
  private RedisQosProfileService qosProfileService;

  @Test
  void testGetQosProfiles_getAll_ok() {
    List<QosProfile> qosProfiles = qosProfileService.getQosProfiles(null, null);
    assertEquals(4, qosProfiles.size());
    assertEquals(QosProfileStatusEnum.ACTIVE, qosProfiles.get(0).getStatus());
  }

  @Test
  void testGetQosProfiles_ByNameAndStatus_ok() {
    List<QosProfile> qosProfiles = qosProfileService.getQosProfiles("QOS_E", QosProfileStatusEnum.ACTIVE);
    assertEquals(1, qosProfiles.size());
    assertEquals("QOS_E", qosProfiles.get(0).getName());
    assertEquals(QosProfileStatusEnum.ACTIVE, qosProfiles.get(0).getStatus());
  }

  @Test
  void testGetQosProfiles_ByStatus_ok() {
    List<QosProfile> qosProfiles = qosProfileService.getQosProfiles(null, QosProfileStatusEnum.ACTIVE);
    assertEquals(4, qosProfiles.size()); // Assuming all test data profiles have ACTIVE status
  }

  @Test
  void testGetQosProfiles_ByStatus_notFound_404() {
    QodApiException exception = assertThrows(QodApiException.class,
        () -> qosProfileService.getQosProfiles(null, QosProfileStatusEnum.DEPRECATED));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    assertEquals("No QoS Profiles found", exception.getMessage());
  }

  @Test
  void testGetQosProfiles_ByName_ok() {
    List<QosProfile> qosProfiles = qosProfileService.getQosProfiles("QOS_S", null);
    assertEquals(1, qosProfiles.size());
    assertEquals("QOS_S", qosProfiles.get(0).getName());
  }

  @Test
  void testGetQosProfiles_ByName_notFound_404() {
    QodApiException exception = assertThrows(QodApiException.class, () -> qosProfileService.getQosProfiles("NonExistent", null));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    assertEquals("QosProfile Id does not exist", exception.getMessage());
  }

  @Test
  void testGetQosProfile_ok() {
    QosProfile qosProfile = qosProfileService.getQosProfile("QOS_M");
    assertEquals("QOS_M", qosProfile.getName());
  }

  @Test
  void testGetQosProfile_notFound_404() {
    QodApiException exception = assertThrows(QodApiException.class, () -> qosProfileService.getQosProfile("NonExistent"));
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    assertEquals("QosProfile Id does not exist", exception.getMessage());
  }
}
