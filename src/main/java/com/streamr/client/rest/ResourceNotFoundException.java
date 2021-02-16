package com.streamr.client.rest;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String resourceName) {
    super("Resource not found: " + resourceName);
  }
}
