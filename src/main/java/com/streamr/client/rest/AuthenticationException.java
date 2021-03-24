package com.streamr.client.rest;

public class AuthenticationException extends RuntimeException {
  public AuthenticationException(final String resourceName) {
    super("Authentication failed: " + resourceName);
  }
}
