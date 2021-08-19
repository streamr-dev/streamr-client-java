package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.streamr.client.protocol.utils.Address;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import okio.Buffer;
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

  private static JsonReader toReader(String json) {
    try (Buffer buffer = new Buffer()) {
      return JsonReader.of(buffer.writeString(json, StandardCharsets.UTF_8));
    }
  }

  private JsonAdapter<TestWrapper> adapter =
      new Moshi.Builder()
          .add(Address.class, new AddressJsonAdapter().nullSafe())
          .build()
          .adapter(TestWrapper.class);

  @ParameterizedTest(name = "from json {0} to object")
  @MethodSource("testDataProviderFromJson")
  void fromJson(String json, Address address) throws IOException {
    assertEquals(address, adapter.fromJson(toReader(json)).address);
  }

  static Stream<Arguments> testDataProviderFromJson() {
    return Stream.of(
        arguments(
            "{\"address\": \"0x1111111111111111111111111111111111111111\"}",
            new Address("0x1111111111111111111111111111111111111111")),
        arguments("{\"address\": null}", null),
        arguments("{}", null));
  }

  @ParameterizedTest(name = "from object {0} to json")
  @MethodSource("testDataProviderToJson")
  void toJson(Address address, String json) {
    assertEquals(json, adapter.toJson(new TestWrapper(address)));
  }

  static Stream<Arguments> testDataProviderToJson() {
    return Stream.of(
        arguments(
            new Address("0x2222222222222222222222222222222222222222"),
            "{\"address\":\"0x2222222222222222222222222222222222222222\"}"),
        arguments(null, "{}"));
  }
}
