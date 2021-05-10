package com.streamr.client.utils;

public class SignatureFailedException extends RuntimeException {
    public SignatureFailedException(String message) {
        super(message);
    }
}
