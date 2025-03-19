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

package com.camara.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import lombok.Generated;

@Generated
public class CloudEventSerializer extends JsonSerializer<CloudEvent> {

  @Override
  public void serialize(CloudEvent cloudEvent, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeStartObject();
    gen.writeStringField("id", cloudEvent.getId());
    gen.writeStringField("source", cloudEvent.getSource().toString());
    gen.writeStringField("type", cloudEvent.getType());
    gen.writeStringField("datacontenttype", cloudEvent.getDataContentType());
    gen.writeStringField("subject", cloudEvent.getSubject());
    OffsetDateTime time = cloudEvent.getTime();
    if (time != null) {
      gen.writeStringField("time", time.toString());
    }
    gen.writeStringField("specversion", cloudEvent.getSpecVersion().toString());
    gen.writeFieldName("data");
    // Handle nullable data field
    CloudEventData cloudEventData = cloudEvent.getData();
    if (cloudEventData != null) {
      gen.writeRawValue(new String(cloudEventData.toBytes(), StandardCharsets.UTF_8));
    } else {
      gen.writeNull();
    }
    gen.writeEndObject();
  }
}
