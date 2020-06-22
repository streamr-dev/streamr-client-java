package com.streamr.client.utils;

import com.streamr.client.exceptions.MalformedMessageException;

public class ValidationUtil {
    public static void checkNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new MalformedMessageException(fieldName + " can not be null");
        }
    }
}
