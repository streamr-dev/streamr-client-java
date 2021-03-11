package com.streamr.client.rest;

public class AuthenticationException extends RuntimeException {
  public AuthenticationException(String resourceName) {
    super("Authentication failed: " + resourceName);
  }
}
