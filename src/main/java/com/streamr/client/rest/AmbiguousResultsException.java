package com.streamr.client.rest;

public class AmbiguousResultsException extends RuntimeException {
  public AmbiguousResultsException(final String message) {
    super(message);
  }
}
