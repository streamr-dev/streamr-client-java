package com.streamr.client.protocol.message_layer;

import java.util.Collection;

class ValidationUtil {
  static void checkNotNull(Object value, String fieldName) {
    if (value == null) {
      throw new MalformedMessageException(fieldName + " can not be null");
    }
  }

  static <T> void checkNotEmpty(Collection<T> collection, String fieldName) {
    if (collection.isEmpty()) {
      throw new MalformedMessageException(fieldName + " can not be empty");
    }
  }
}
