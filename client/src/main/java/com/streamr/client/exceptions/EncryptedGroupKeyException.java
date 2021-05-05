package com.streamr.client.exceptions;

public class EncryptedGroupKeyException extends RuntimeException {

    public EncryptedGroupKeyException() {
        super("Cannot use an encrypted group key to encrypt or decrypt.");
    }

}
