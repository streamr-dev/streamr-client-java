package com.streamr.client.rest;

public class PermissionDeniedException extends RuntimeException {

  public PermissionDeniedException(String resourceName) {
    super("Permission denied: " + resourceName);
  }
}
