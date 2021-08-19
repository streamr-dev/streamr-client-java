package com.streamr.client.protocol.message_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.streamr.client.protocol.utils.EncryptedGroupKey;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GroupKeyAnnounceAdapterTest {
  private static final EncryptedGroupKey key1 = new EncryptedGroupKey("groupKeyId1", "encrypted1");
  private static final EncryptedGroupKey key2 = new EncryptedGroupKey("groupKeyId2", "encrypted2");

  @Test
  void serializationAndDeserialization() {
    String expected =
        "[\"streamId\",[[\"groupKeyId1\",\""
            + key1.getEncryptedGroupKeyHex()
            + "\"],[\"groupKeyId2\",\""
            + key2.getEncryptedGroupKeyHex()
            + "\"]]]";
    GroupKeyAnnounce message = new GroupKeyAnnounce("streamId", Arrays.asList(key1, key2));
    AbstractGroupKeyMessage deserialize =
        AbstractGroupKeyMessage.deserialize(expected, StreamMessage.MessageType.GROUP_KEY_ANNOUNCE);
    assertEquals(message, deserialize);
    assertEquals(expected, message.serialize());
  }
}
