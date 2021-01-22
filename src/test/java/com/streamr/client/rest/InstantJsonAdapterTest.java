package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InstantJsonAdapterTest {
  private final InstantJsonAdapter adapter = new InstantJsonAdapter();
  private static final String[] testData =
      new String[] {
        "2021-01-08T12:42:00.056Z",
      };

  static List<Arguments> instants() {
    final List<Arguments> result = new ArrayList<>();
    result.add(Arguments.of(null, null));
    for (String expected : testData) {
      final Instant input = Instant.parse(expected);
      result.add(Arguments.of(input, expected));
    }
    return result;
  }

  static List<Arguments> strings() {
    final List<Arguments> result = new ArrayList<>();
    for (Arguments args : instants()) {
      final Object[] arguments = args.get();
      result.add(Arguments.of(arguments[1], arguments[0]));
    }
    return result;
  }

  @DisplayName("Convert java.time.Instant to JSON String")
  @ParameterizedTest(name = "with java.util.Instant {1}")
  @MethodSource("instants")
  void instantToString(final Instant input, final String expected) {
    assertEquals(expected, adapter.toJson(input));
  }

  @DisplayName("Convert JSON String to java.time.Instant")
  @ParameterizedTest(name = "with String {0}")
  @MethodSource("strings")
  void stringToInstant(final String input, final Instant expected) {
    assertEquals(expected, adapter.fromJson(input));
  }
}
