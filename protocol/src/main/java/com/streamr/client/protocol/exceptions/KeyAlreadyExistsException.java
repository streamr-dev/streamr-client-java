package com.streamr.client.protocol.exceptions;

import com.streamr.client.protocol.utils.GroupKey;

public class KeyAlreadyExistsException extends RuntimeException {
  public KeyAlreadyExistsException(GroupKey key) {
    super("Key " + key.getGroupKeyId() + " already exists in this GroupKeyStore!");
  }
}
