package com.streamr.client.exceptions;

public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException(String resourceName) {
        super("Authentication required: " + resourceName);
    }

}
