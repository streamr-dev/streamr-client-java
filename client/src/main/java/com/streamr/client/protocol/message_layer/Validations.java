package com.streamr.client.protocol.message_layer;

import java.util.Collection;
import java.util.Objects;

final class Validations {
  private Validations() {}

  static <T> void checkNotEmpty(final Collection<T> collection, final String fieldName) {
    Objects.requireNonNull(collection, fieldName);
    if (collection.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " can not be empty");
    }
  }
}
