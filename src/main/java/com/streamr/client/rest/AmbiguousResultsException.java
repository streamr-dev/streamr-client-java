package com.streamr.client.rest;

public class AmbiguousResultsException extends RuntimeException {
  public AmbiguousResultsException(String message) {
    super(message);
  }
}
