package com.streamr.client.utils;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class InstantJsonAdapter {
  @FromJson
  public Instant fromJson(final String s) {
    if (Objects.equals(s, null)) {
      return null;
    }
    return Instant.parse(s);
  }

  @ToJson
  public String toJson(final Instant value) {
    if (Objects.equals(value, null)) {
      return null;
    }
    return DateTimeFormatter.ISO_INSTANT.format(value);
  }
}
