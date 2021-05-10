package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.streamr.client.utils.Address;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import okio.Buffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AddressJsonAdapterTest {
  private static class TestWrapper {
    Address address;

    TestWrapper(Address address) {
      this.address = address;
    }
  }

  private final JsonAdapter<TestWrapper> adapter =
      new Moshi.Builder()
          .add(Address.class, new AddressJsonAdapter().nullSafe())
          .build()
          .adapter(TestWrapper.class);

  private static JsonReader toReader(String json) {
    Buffer buffer = new Buffer();
    try {
      return JsonReader.of(buffer.writeString(json, StandardCharsets.UTF_8));
    } finally {
      buffer.close();
    }
  }

  static List<Arguments> fromJsonTestCases() {
    List<Arguments> list = new ArrayList<>();
    list.add(
        Arguments.of(
            "{\"address\":\"0x1111111111111111111111111111111111111111\"}",
            new Address("0x1111111111111111111111111111111111111111")));
    list.add(Arguments.of("{\"address\":null}", null));
    list.add(Arguments.of("{}", null));
    return list;
  }

  @DisplayName("Convert JSON String to Address")
  @ParameterizedTest(name = "with String {0}")
  @MethodSource("fromJsonTestCases")
  void fromJson(String json, Address address) throws IOException {
    TestWrapper wrapper = adapter.fromJson(toReader(json));
    assertEquals(address, wrapper.address);
  }

  static List<Arguments> toJsonTestCases() {
    List<Arguments> list = new ArrayList<>();
    list.add(
        Arguments.of(
            new Address("0x1111111111111111111111111111111111111111"),
            "{\"address\":\"0x1111111111111111111111111111111111111111\"}"));
    list.add(Arguments.of(null, "{}"));
    return list;
  }

  @DisplayName("Convert Address to JSON String")
  @ParameterizedTest(name = "with Address {0}")
  @MethodSource("toJsonTestCases")
  void toJson(Address address, String json) {
    TestWrapper wrapper = new TestWrapper(address);
    assertEquals(json, adapter.toJson(wrapper));
  }
}
