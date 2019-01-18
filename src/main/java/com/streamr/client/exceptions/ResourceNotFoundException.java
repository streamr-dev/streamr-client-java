package com.streamr.client.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName) {
        super("Resource not found: " + resourceName);
    }

}
