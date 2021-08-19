package com.streamr.client.protocol.message_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.streamr.client.protocol.utils.EncryptedGroupKey;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GroupKeyResponseAdapterTest {
  private static final EncryptedGroupKey key1 = new EncryptedGroupKey("groupKeyId1", "encrypted1");
  private static final EncryptedGroupKey key2 = new EncryptedGroupKey("groupKeyId2", "encrypted2");

  @Test
  void serializationAndDeserialization() {
    String serializedMessage =
        "[\"requestId\",\"streamId\",[[\"groupKeyId1\",\""
            + key1.getEncryptedGroupKeyHex()
            + "\"],[\"groupKeyId2\",\""
            + key2.getEncryptedGroupKeyHex()
            + "\"]]]";
    GroupKeyResponse message =
        new GroupKeyResponse("requestId", "streamId", Arrays.asList(key1, key2));

    AbstractGroupKeyMessage deserialize =
        AbstractGroupKeyMessage.deserialize(
            serializedMessage, StreamMessage.MessageType.GROUP_KEY_RESPONSE);
    assertEquals(message, deserialize);
    assertEquals(serializedMessage, message.serialize());
  }
}
