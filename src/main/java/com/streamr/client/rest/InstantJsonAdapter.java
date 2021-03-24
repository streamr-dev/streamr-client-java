package com.streamr.client.rest;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;
import com.streamr.client.java.util.Objects;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

final class InstantJsonAdapter {
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
