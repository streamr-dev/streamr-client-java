package com.streamr.client.protocol.message_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.streamr.ethereum.common.Address;
import java.io.IOException;
import okio.Buffer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

class MessageIdAdapterTest {
  final int partition;
  final long timestamp;
  final long sequenceNumber;
  final String streamId;
  final String publishedId;

  {
    partition = 99;
    timestamp = System.currentTimeMillis();
    sequenceNumber = 953279L;
    final Address a = new Address("0x6CF8d65EDbb7625c0867ff8e2562C1cB37037651");
    streamId = a.toString() + "/foobar";
    publishedId = a.toString();
  }

  final String msgChainId = RandomStringUtils.randomAlphanumeric(20);
  final String json =
      String.format(
          "[\"%s\",%d,%d,%d,\"%s\",\"%s\"]",
          streamId, partition, timestamp, sequenceNumber, publishedId, msgChainId);
  final MessageIdAdapter adapter = new MessageIdAdapter();
  final MessageId messageId =
      new MessageId.Builder()
          .withStreamId(streamId)
          .withStreamPartition(partition)
          .withTimestamp(timestamp)
          .withSequenceNumber(sequenceNumber)
          .withPublisherId(new Address(publishedId))
          .withMsgChainId(msgChainId)
          .createMessageId();

  @Test
  void fromJson() throws IOException {
    assertEquals(messageId, adapter.fromJson(json));
  }

  @Test
  void toJson() throws IOException {
    try (Buffer writer = new Buffer()) {
      adapter.toJson(writer, messageId);
      assertEquals(json, writer.readUtf8());
    }
  }
}
