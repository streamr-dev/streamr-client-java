package com.streamr.client.protocol.message_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.streamr.client.testing.StreamMessageExamples;
import com.streamr.client.testing.TestingContentX;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreamMessageTest {
  private StreamMessage msg;

  @BeforeEach
  void setup() {
    msg = StreamMessage.deserialize(StreamMessageExamples.InvalidSignature.helloWorldSerialized32);
  }

  @Test
  void constructorThatTakesMapContentSetsTheCorrectSerializedContent() {
    String serializedContent = msg.getSerializedContent();
    StreamMessage.Content content = TestingContentX.fromJsonString(serializedContent);
    msg = new StreamMessage.Builder(msg).withContent(content).createStreamMessage();
    assertEquals(content.toMap(), msg.getParsedContent());
    assertEquals(serializedContent, msg.getSerializedContent());
  }

  @Test
  void getParsedContentThrowsIfMessageIsAESEncrypted() {
    msg =
        new StreamMessage.Builder(msg)
            .withEncryptionType(StreamMessage.EncryptionType.AES)
            .createStreamMessage();
    assertThrows(
        EncryptedContentNotParsableException.class,
        () -> {
          msg.getParsedContent();
        });
  }

  @Test
  void equalsContract() {
    EqualsVerifier.forClass(StreamMessage.class).suppress(Warning.NULL_FIELDS).verify();
  }
}
