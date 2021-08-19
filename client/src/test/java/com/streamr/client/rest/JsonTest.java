package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class JsonTest {
  @Test
  void withValue() throws IOException {
    String from = "{\"foo\": \"x\", \"bar\": \"123\"}";
    String to = Json.withValue(from, "foo", "y");
    assertEquals("{\"foo\":\"y\",\"bar\":\"123\"}", to);
  }

  @Test
  void withValue_noPreviousValue() throws IOException {
    String from = "{ \"bar\": \"456\"}";
    String to = Json.withValue(from, "foo", "z");
    assertEquals("{\"bar\":\"456\",\"foo\":\"z\"}", to);
  }

  @Test
  void withValue_array() {
    assertThrows(
        IOException.class,
        () -> {
          String from = "[]";
          Json.withValue(from, "foo", "x");
        });
  }

  @Test
  void withValue_invalidJson() {
    assertThrows(
        IOException.class,
        () -> {
          String from = "invalid-data";
          Json.withValue(from, "foo", "x");
        });
  }
}
