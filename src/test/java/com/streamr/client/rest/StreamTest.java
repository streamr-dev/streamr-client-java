package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.moshi.JsonReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import nl.jqno.equalsverifier.EqualsVerifier;
import okio.Buffer;
import org.junit.jupiter.api.Test;

class StreamTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(Stream.class).verify();
  }

  private static JsonReader toReader(String json) {
    try (Buffer buffer = new Buffer()) {
      return JsonReader.of(buffer.writeString(json, StandardCharsets.UTF_8));
    }
  }

  @Test
  void streamObjectsParseCorrectly() throws IOException {
    String json =
        "{\"id\":\"7wa7APtlTq6EC5iTCBy6dw\",\"partitions\":1,\"name\":\"Public transport demo\",\"feed\":{\"id\":7,\"name\":\"API\",\"module\":147},\"config\":{\"topic\":\"7wa7APtlTq6EC5iTCBy6dw\",\"fields\":[{\"name\":\"veh\",\"type\":\"string\"},{\"name\":\"spd\",\"type\":\"number\"},{\"name\":\"hdg\",\"type\":\"number\"},{\"name\":\"lat\",\"type\":\"number\"},{\"name\":\"long\",\"type\":\"number\"},{\"name\":\"line\",\"type\":\"string\"}]},\"description\":\"Helsinki tram data by HSL\",\"uiChannel\":false,\"dateCreated\":\"2016-05-27T15:46:30Z\",\"lastUpdated\":\"2017-04-10T16:04:38Z\"}";

    Stream s = StreamrRESTClient.streamJsonAdapter.fromJson(toReader(json));

    assertEquals("7wa7APtlTq6EC5iTCBy6dw", s.getId());
    assertEquals("Public transport demo", s.getName());
    assertEquals(6, s.getConfig().getFields().size());
  }
}
