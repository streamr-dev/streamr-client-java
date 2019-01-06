package com.streamr.client.exceptions;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String resourceName) {
        super("Authentication failed: " + resourceName);
    }

}
