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

package com.camara.qod.controller;

import static com.camara.qod.util.QosProfilesTestData.NAME_PARAMETER;
import static com.camara.qod.util.QosProfilesTestData.QOS_PROFILES_URI;
import static com.camara.qod.util.QosProfilesTestData.STATUS_PARAMETER;
import static com.camara.qod.util.QosProfilesTestData.getQosProfilesTestData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camara.qod.annotation.UnsecuredWebMvcTest;
import com.camara.qod.api.QosProfilesApiController;
import com.camara.qod.api.model.QosProfile;
import com.camara.qod.exception.ExceptionHandlerAdvice;
import com.camara.qod.security.SecurityConfig;
import com.camara.qod.service.H2QosProfileService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@UnsecuredWebMvcTest(controllers = QosProfilesApiController.class)
@Import(value = {
    QosProfilesController.class,
    ExceptionHandlerAdvice.class,
    SecurityConfig.class

})
@ContextConfiguration(classes = QosProfilesApiController.class)
class QosProfilesControllerTest {

  @MockBean
  H2QosProfileService qosProfileService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void testGetQosProfiles_All_200() throws Exception {
    List<QosProfile> qosProfilesTestData = getQosProfilesTestData();
    when(qosProfileService.getQosProfiles(anyString(), any())).thenReturn(qosProfilesTestData);
    mockMvc.perform(MockMvcRequestBuilders
            .get(QOS_PROFILES_URI)
            .param(NAME_PARAMETER, "")
            .param(STATUS_PARAMETER, ""))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.size()").value(qosProfilesTestData.size()))
        .andExpect(jsonPath("$[0].name").value(qosProfilesTestData.get(0).getName()))
        .andExpect(jsonPath("$[3].name").value(qosProfilesTestData.get(3).getName()));

  }

  @Test
  void testGetQosProfile_ByName_200() throws Exception {
    QosProfile qosProfile = getQosProfilesTestData().get(0);
    when(qosProfileService.getQosProfile(anyString())).thenReturn(qosProfile);
    mockMvc.perform(MockMvcRequestBuilders
            .get(QOS_PROFILES_URI + "/" + qosProfile.getName()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(qosProfile.getName()))
        .andExpect(jsonPath("$.description").value(qosProfile.getDescription()))
        .andExpect(jsonPath("$.status").value(qosProfile.getStatus().name()));

  }


}
