package com.streamr.client.utils;

public class SigningRequiredException extends RuntimeException {

    public SigningRequiredException(String message) {
        super(message);
    }

}
