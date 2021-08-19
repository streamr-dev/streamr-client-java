package com.streamr.client.protocol.exceptions;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class UnsupportedSignatureTypeException extends RuntimeException {
  public UnsupportedSignatureTypeException(StreamMessage.SignatureType signatureType) {
    super("Unsupported signature type: " + signatureType.getId());
  }
}
