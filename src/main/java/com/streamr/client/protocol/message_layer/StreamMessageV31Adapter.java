package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.protocol.message_layer.StreamMessage.EncryptionType;
import com.streamr.client.protocol.message_layer.StreamMessage.SignatureType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StreamMessageV31Adapter extends JsonAdapter<StreamMessage> {

    private static final Logger log = LoggerFactory.getLogger(StreamMessageV31Adapter.class);

    private static final int VERSION = 31;
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
            StreamMessage.MessageType messageType = StreamMessage.MessageType.fromId((byte)reader.nextInt());
            EncryptionType encryptionType = EncryptionType.fromId((byte)reader.nextInt());
            String serializedContent = reader.nextString();
            SignatureType signatureType = SignatureType.fromId((byte)reader.nextInt());
            String signature = null;
            if (signatureType != SignatureType.NONE) {
                signature = reader.nextString();
            } else {
                reader.nextNull();
            }

            return new StreamMessage.Builder().setMessageID(messageID).setPreviousMessageRef(previousMessageRef).setMessageType(messageType).setSerializedContent(serializedContent).setContentType(StreamMessage.ContentType.JSON).setEncryptionType(encryptionType).setGroupKeyId(null).setNewGroupKey(null).setSignatureType(signatureType).setSignature(signature).createStreamMessage();
        } catch (JsonDataException e) {
            log.error("Failed to parse StreamMessageV31", e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    @Override
    public void toJson(@NotNull JsonWriter writer, StreamMessage msg) throws IOException {
        throw new RuntimeException("Serializing to old version is not supported");
    }
}
