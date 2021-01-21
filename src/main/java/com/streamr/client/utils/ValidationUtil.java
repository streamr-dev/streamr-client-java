package com.streamr.client.utils;

import com.streamr.client.protocol.message_layer.MalformedMessageException;

import java.util.Collection;

public class ValidationUtil {
    public static void checkNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new MalformedMessageException(fieldName + " can not be null");
        }
    }
    public static <T> void checkNotEmpty(Collection<T> collection, String fieldName) {
        if (collection.isEmpty()) {
            throw new MalformedMessageException(fieldName + " can not be empty");
        }
    }
}
