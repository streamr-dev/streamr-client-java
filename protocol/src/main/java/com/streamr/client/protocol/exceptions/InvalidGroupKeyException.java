package com.streamr.client.protocol.exceptions;

public class InvalidGroupKeyException extends Exception {
  public InvalidGroupKeyException(int keyLength) {
    super(
        String.format(
            "Group key must be 256 bits long, but got a key length of %d bits.", keyLength));
  }
}
