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

package com.camara.controller;

import static com.camara.util.QosProfilesTestData.RETRIEVE_QOS_PROFILES_URI;
import static com.camara.util.QosProfilesTestData.RETRIEVE_QOS_PROFILE_BY_NAME_URI;
import static com.camara.util.QosProfilesTestData.getQosProfilesTestData;
import static com.camara.util.SessionsTestData.TEST_DEVICE_IPV4_ADDRESS;
import static com.camara.util.TestData.getAsJsonFormat;
import static com.camara.util.TestData.objectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camara.annotation.UnsecuredWebMvcTest;
import com.camara.exception.ExceptionHandlerAdvice;
import com.camara.qos_profiles.api.QoSProfilesApiController;
import com.camara.qos_profiles.api.model.Device;
import com.camara.qos_profiles.api.model.DeviceIpv4Addr;
import com.camara.qos_profiles.api.model.QosProfile;
import com.camara.qos_profiles.api.model.QosProfileDeviceRequest;
import com.camara.security.SecurityStandardConfig;
import com.camara.service.QosProfileService;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@UnsecuredWebMvcTest(controllers = QoSProfilesApiController.class)
@Import(value = {
    QosProfilesController.class,
    ExceptionHandlerAdvice.class,
    SecurityStandardConfig.class
})
@ContextConfiguration(classes = QoSProfilesApiController.class)
class QosProfilesControllerTest {

  @MockitoBean
  private QosProfileService qosProfileService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void testRetrieveQosProfile_ByName_200() throws Exception {
    QosProfile qosProfile = getQosProfilesTestData().getFirst();
    when(qosProfileService.getQosProfile(anyString())).thenReturn(qosProfile);
    mockMvc.perform(MockMvcRequestBuilders
            .get(RETRIEVE_QOS_PROFILE_BY_NAME_URI + "/" + qosProfile.getName()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(qosProfile.getName()))
        .andExpect(jsonPath("$.description").value(qosProfile.getDescription()))
        .andExpect(jsonPath("$.status").value(qosProfile.getStatus().name()));
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void testRetrieveQosProfile_ByInvalidName_400(CapturedOutput output) throws Exception {
    mockMvc.perform(MockMvcRequestBuilders
            .get(RETRIEVE_QOS_PROFILE_BY_NAME_URI + "/&!INVALID=D"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed for property <getQosProfile.name> with value <&!INVALID=D>"));
    assertTrue(output.getAll().contains("^[a-zA-Z0-9_.-]+$"));
  }

  @Test
  void testRetrieveQosProfilesForDevice_200() throws Exception {
    QosProfileDeviceRequest request = new QosProfileDeviceRequest()
        .device(new Device().ipv4Address(new DeviceIpv4Addr().publicAddress(TEST_DEVICE_IPV4_ADDRESS)));
    when(qosProfileService.getQosProfiles(any(), any())).thenReturn(getQosProfilesTestData());

    var response = mockMvc.perform(MockMvcRequestBuilders
            .post(RETRIEVE_QOS_PROFILES_URI)
            .content(getAsJsonFormat(request))
            .contentType(MediaType.APPLICATION_JSON)
        )
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andReturn();

    List<QosProfile> qosProfiles = objectMapper.readValue(response.getResponse().getContentAsString(),
        new TypeReference<>() {
        });
    assertEquals(4, qosProfiles.size());
  }


}
