package com.streamr.client.rest;

import org.apache.groovy.json.internal.IO;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTest {
  @Test
  public void withValue() throws IOException {
    String from = "{\"foo\": \"x\", \"bar\": \"123\"}";
    String to = Json.withValue(from, "foo", "y");
    assertEquals("{\"foo\":\"y\",\"bar\":\"123\"}", to);
  }

  @Test
  public void withValue_noPreviousValue() throws IOException {
    String from = "{ \"bar\": \"456\"}";
    String to = Json.withValue(from, "foo", "z");
    assertEquals("{\"bar\":\"456\",\"foo\":\"z\"}", to);
  }

  @Test
  public void withValue_array() {
    assertThrows(IOException.class, () -> {
      String from = "[]";
      Json.withValue(from, "foo", "x");
    });
  }

  @Test
  public void withValue_invalidJson() {
    assertThrows(IOException.class, () -> {
      String from = "invalid-data";
      Json.withValue(from, "foo", "x");
    });
  }
}