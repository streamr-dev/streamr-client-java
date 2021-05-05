package com.streamr.client.exceptions;

public class SignatureFailedException extends RuntimeException {
    public SignatureFailedException(String message) {
        super(message);
    }
}
