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

package com.camara.qod.plugin.storage;

import com.camara.datatypes.model.QosSession;
import com.camara.qod.plugin.storage.repository.QodSessionRepository;
import com.camara.qod.api.model.CreateSession;
import com.camara.qod.api.model.Protocol;
import com.camara.qod.api.model.QosProfile;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@SpringBootTest(classes = {QodSessionRepository.class, StorageImpl.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class StorageTests {

    private StorageImpl storageImpl;

    @Test
    void getUnknownSession() {
        // TODO: QodSessionRepository in StorageImpl is not correctly initialised and null --> fix
        storageImpl.getSession(UUID.randomUUID());
        //assertThrows(NullPointerException.class, () -> storage.getSession(UUID.randomUUID()));
    }

    @Test
    void createAndDeleteSession() {
        long now = Instant.now().getEpochSecond();
        int duration = 2;
        QosSession session = storageImpl.saveSession(now, now + duration, UUID.randomUUID(), session(), "subscrId123", UUID.randomUUID());
        storageImpl.deleteSession(session.getId());
    }

    private CreateSession session() {
        return new CreateSession()
                .ueAddr("172.24.11.4")
                .asAddr("200.24.24.2")
                .duration(2)
                .uePorts(null)
                .protocolIn(Protocol.ANY)
                .protocolOut(Protocol.ANY)
                .qos(QosProfile.LOW_LATENCY)
                .notificationUri(URI.create("http://example.com"))
                .notificationAuthToken("12345");
    }

}
