package com.streamr.client.exceptions;

public class SigningRequiredException extends RuntimeException {

    public SigningRequiredException(String message) {
        super(message);
    }

}
