package com.streamr.client.rest;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(final String resourceName) {
    super("Resource not found: " + resourceName);
  }
}
