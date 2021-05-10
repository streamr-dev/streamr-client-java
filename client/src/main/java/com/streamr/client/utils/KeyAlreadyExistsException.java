package com.streamr.client.utils;

import com.streamr.client.utils.GroupKey;

public class KeyAlreadyExistsException extends RuntimeException {
    public KeyAlreadyExistsException(GroupKey key) {
        super("Key " + key.getGroupKeyId() + " already exists in this GroupKeyStore!");
    }
}
