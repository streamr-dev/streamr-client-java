package com.streamr.client.rest;

public class PermissionDeniedException extends RuntimeException {
  public PermissionDeniedException(final String resourceName) {
    super("Permission denied: " + resourceName);
  }
}
