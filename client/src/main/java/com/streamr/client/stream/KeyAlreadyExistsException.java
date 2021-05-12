package com.streamr.client.stream;

public class KeyAlreadyExistsException extends RuntimeException {
    public KeyAlreadyExistsException(GroupKey key) {
        super("Key " + key.getGroupKeyId() + " already exists in this GroupKeyStore!");
    }
}
