package com.streamr.client.subs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.protocol.exceptions.InvalidGroupKeyException;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.utils.EncryptionUtil;
import com.streamr.client.protocol.utils.GroupKey;
import com.streamr.client.testing.TestingAddresses;
import com.streamr.client.testing.TestingContent;
import com.streamr.client.testing.TestingMessageRef;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DecryptionQueuesTest {
  private DecryptionQueues decryptionQueues;

  @BeforeEach
  void setup() {
    decryptionQueues = new DecryptionQueues("streamId", 0);
  }

  @Test
  void messagesAreQueuedByMsgChainIdAndTheyCanBeDrainedByGroupKeyIds()
      throws InvalidGroupKeyException {
    GroupKey key1 = GroupKey.generate();
    GroupKey key2 = GroupKey.generate();
    GroupKey pub2key = GroupKey.generate();

    final StreamMessage.Content content = TestingContent.emptyMessage();
    // msgChain1 has messages with two different keys
    final MessageId messageId6 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(0)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChain1")
            .createMessageId();
    StreamMessage chain1key1msg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId6)
            .withContent(content)
            .createStreamMessage();
    final MessageId messageId5 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChain1")
            .createMessageId();
    StreamMessage chain1key1msg2 =
        new StreamMessage.Builder()
            .withMessageId(messageId5)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(0L, 0L))
            .withContent(content)
            .createStreamMessage();
    final MessageId messageId4 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(2)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChain1")
            .createMessageId();
    StreamMessage chain1key1msg3 =
        new StreamMessage.Builder()
            .withMessageId(messageId4)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(0L, 0L))
            .withContent(content)
            .createStreamMessage();
    final MessageId messageId3 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(3)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChain1")
            .createMessageId();
    StreamMessage chain1key2msg4 =
        new StreamMessage.Builder()
            .withMessageId(messageId3)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(0L, 0L))
            .withContent(content)
            .createStreamMessage();

    // Also there's another msgChain from the same publisher
    final MessageId messageId2 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(0)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChain2")
            .createMessageId();
    StreamMessage chain2key2msg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId2)
            .withContent(content)
            .createStreamMessage();
    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChain2")
            .createMessageId();
    StreamMessage chain2key2msg2 =
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(0L, 0L))
            .withContent(content)
            .createStreamMessage();

    // And a completely different publisher
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(0)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(2))
            .withMsgChainId("pub2msgChain")
            .createMessageId();
    StreamMessage pub2msg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withContent(content)
            .createStreamMessage();

    // Encrypt each message with appropriate key and add to the decryptionQueues
    chain1key1msg1 = EncryptionUtil.encryptStreamMessage(chain1key1msg1, key1);
    chain1key1msg2 = EncryptionUtil.encryptStreamMessage(chain1key1msg2, key1);
    chain1key1msg3 = EncryptionUtil.encryptStreamMessage(chain1key1msg3, key1);
    decryptionQueues.add(chain1key1msg1);
    decryptionQueues.add(chain1key1msg2);
    decryptionQueues.add(chain1key1msg3);

    chain1key2msg4 = EncryptionUtil.encryptStreamMessage(chain1key2msg4, key2);
    chain2key2msg1 = EncryptionUtil.encryptStreamMessage(chain2key2msg1, key2);
    chain2key2msg2 = EncryptionUtil.encryptStreamMessage(chain2key2msg2, key2);
    decryptionQueues.add(chain1key2msg4);
    decryptionQueues.add(chain2key2msg1);
    decryptionQueues.add(chain2key2msg2);

    pub2msg1 = EncryptionUtil.encryptStreamMessage(pub2msg1, pub2key);
    decryptionQueues.add(pub2msg1);

    assertTrue(!decryptionQueues.isEmpty());

    // Drain with key 1
    Set<String> set1 = new HashSet<>();
    set1.add(key1.getGroupKeyId());
    List<StreamMessage> unlockedByKey1 =
        decryptionQueues.drainUnlockedMessages(TestingAddresses.PUBLISHER_ID, set1);
    List<StreamMessage> expected1 = new ArrayList<>();
    expected1.add(chain1key1msg1);
    expected1.add(chain1key1msg2);
    expected1.add(chain1key1msg3);
    assertEquals(expected1, unlockedByKey1);
    assertTrue(!decryptionQueues.isEmpty());

    // Drain with key 2
    Set<String> set2 = new HashSet<>();
    set2.add(key2.getGroupKeyId());
    List<StreamMessage> unlockedByKey2 =
        decryptionQueues.drainUnlockedMessages(TestingAddresses.PUBLISHER_ID, set2);
    List<StreamMessage> expected2 = new ArrayList<>();
    expected2.add(chain1key2msg4);
    expected2.add(chain2key2msg1);
    expected2.add(chain2key2msg2);
    assertEquals(expected2, unlockedByKey2);
    assertTrue(!decryptionQueues.isEmpty());

    // Drain with publisher2's key
    Set<String> set3 = new HashSet<>();
    set3.add(pub2key.getGroupKeyId());
    List<StreamMessage> unlockedByPub2 =
        decryptionQueues.drainUnlockedMessages(TestingAddresses.createPublisherId(2), set3);
    List<StreamMessage> expected3 = new ArrayList<>();
    expected3.add(pub2msg1);
    assertEquals(expected3, unlockedByPub2);
    assertTrue(decryptionQueues.isEmpty());
  }
}
