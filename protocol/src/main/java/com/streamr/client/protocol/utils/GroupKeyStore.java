package com.streamr.client.protocol.utils;

import com.streamr.client.protocol.exceptions.KeyAlreadyExistsException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GroupKeyStore {

  private static final Logger log = LoggerFactory.getLogger(GroupKeyStore.class);

  private final Map<String, GroupKey> currentKey = new HashMap<>();

  /** Returns the most recently added key for streamId. */
  public GroupKey getCurrentKey(String streamId) {
    return currentKey.get(streamId);
  }

  /** Returns true if the given groupKeyId is in the store (for any stream) */
  public abstract boolean contains(String groupKeyId);

  /**
   * Returns a previously added GroupKey for the given streamId and groupKeyId, or null if it does
   * not exist.
   */
  public abstract GroupKey get(String streamId, String groupKeyId);

  /**
   * Adds a new GroupKey to the store. Later on it can be retrieved with get(streamId, groupKeyId).
   * This also makes the key the "current key" on the given stream, making it retrievable via
   * getCurrentKey(streamId).
   *
   * <p>Note that the same GroupKey can not be used in more than one stream. If the same key is
   * added twice, a KeyAlreadyExistsException is thrown.
   */
  public void add(String streamId, GroupKey key) throws KeyAlreadyExistsException {
    log.trace("Adding keyId {} to stream {}", key.getGroupKeyId(), streamId);

    // Check that the group key has not already been added to this store.
    if (contains(key.getGroupKeyId())) {
      throw new KeyAlreadyExistsException(key);
    }
    storeKey(streamId, key);
    currentKey.put(streamId, key);
  }

  /**
   * Should store the key so that contains(groupKeyId) and get(streamId, groupKeyId) will return
   * expected results.
   */
  protected abstract void storeKey(String streamId, GroupKey key);
}
