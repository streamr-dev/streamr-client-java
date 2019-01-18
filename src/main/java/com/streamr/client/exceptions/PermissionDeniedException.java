package com.streamr.client.exceptions;

public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String resourceName) {
        super("Permission denied: " + resourceName);
    }

}
