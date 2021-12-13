package com.streamr.client.protocol.message_layer;

import com.streamr.client.protocol.java.util.Objects;
import java.util.Collection;

class ValidationUtil {
  private ValidationUtil() {}

  static <T> void checkNotEmpty(final Collection<T> collection, final String fieldName) {
    Objects.requireNonNull(collection, fieldName);
    if (collection.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " can not be empty");
    }
  }
}