package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.protocol.message_layer.StreamMessage.MessageType;
import com.streamr.client.protocol.message_layer.StreamMessage.SignatureType;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamMessageV30Adapter extends JsonAdapter<StreamMessage> {

  private static final Logger log = LoggerFactory.getLogger(StreamMessageV30Adapter.class);
  private static final MessageIDAdapter msgIdAdapter = new MessageIDAdapter();
  private static final MessageRefAdapter msgRefAdapter = new MessageRefAdapter();

  @Override
  public StreamMessage fromJson(JsonReader reader) throws IOException {
    try {
      // version field has already been read in StreamMessageAdapter
      MessageID messageID = msgIdAdapter.fromJson(reader);
      MessageRef previousMessageRef = null;
      // Peek at the previousMessageRef, as it can be null
      if (reader.peek().equals(JsonReader.Token.NULL)) {
        reader.nextNull();
      } else {
        previousMessageRef = msgRefAdapter.fromJson(reader);
      }
      MessageType messageType = MessageType.fromId((byte) reader.nextInt());
      String serializedContent = reader.nextString();
      SignatureType signatureType = SignatureType.fromId((byte) reader.nextInt());
      String signature = null;
      if (signatureType != SignatureType.NONE) {
        signature = reader.nextString();
      } else {
        reader.nextNull();
      }

      return new StreamMessage.Builder()
          .withMessageId(messageID)
          .withPreviousMessageRef(previousMessageRef)
          .withMessageType(messageType)
          .withSerializedContent(serializedContent)
          .withContentType(StreamMessage.ContentType.JSON)
          .withEncryptionType(StreamMessage.EncryptionType.NONE)
          .withGroupKeyId(null)
          .withNewGroupKey(null)
          .withSignatureType(signatureType)
          .withSignature(signature)
          .createStreamMessage();
    } catch (JsonDataException e) {
      log.error("Failed to parse StreamMessageV30", e);
      throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
    }
  }

  @Override
  public void toJson(@NotNull JsonWriter writer, StreamMessage msg) throws IOException {
    throw new RuntimeException("Serializing to old version is not supported");
  }
}
