package com.streamr.client.utils

import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV30
import com.streamr.client.rest.Stream
import spock.lang.Specification

class MessageCreationUtilSpec extends Specification {
    MessageCreationUtil msgCreationUtil
    Stream stream

    void setup() {
        msgCreationUtil = new MessageCreationUtil("publisherId", null)
        stream = new Stream("test-stream", "")
        stream.id = "stream-id"
        stream.partitions = 1
    }

    void "fields are set"() {
        Date timestamp = new Date()
        when:
        StreamMessageV30 msg = (StreamMessageV30) msgCreationUtil.createStreamMessage(stream, [foo: "bar"], timestamp, null)
        then:
        msg.getStreamId() == "stream-id"
        msg.getStreamPartition() == 0
        msg.getTimestamp() == timestamp.getTime()
        msg.getSequenceNumber() == 0L
        msg.getPublisherId() == "publisherId"
        msg.getMsgChainId().length() == 20
        msg.previousMessageRef == null
        msg.contentType == StreamMessage.ContentType.CONTENT_TYPE_JSON
        msg.content == [foo: "bar"]
        msg.signatureType == StreamMessage.SignatureType.SIGNATURE_TYPE_NONE
        msg.signature == null
    }

    void "signer is called"() {
        SigningUtil signingUtil = Mock(SigningUtil)
        MessageCreationUtil msgCreationUtil2 = new MessageCreationUtil("publisherId", signingUtil)
        when:
        msgCreationUtil2.createStreamMessage(stream, [foo: "bar"], new Date(), null)
        then:
        1 * signingUtil.signStreamMessage(_)
    }

    void "publish with sequence numbers equal to 0"() {
        long timestamp = (new Date()).getTime()
        when:
        StreamMessageV30 msg1 = (StreamMessageV30) msgCreationUtil.createStreamMessage(stream, [foo: "bar"], new Date(timestamp), null)
        StreamMessageV30 msg2 = (StreamMessageV30) msgCreationUtil.createStreamMessage(stream, [foo: "bar"], new Date(timestamp + 100), null)
        StreamMessageV30 msg3 = (StreamMessageV30) msgCreationUtil.createStreamMessage(stream, [foo: "bar"], new Date(timestamp + 200), null)
        then:
        msg1.getTimestamp() == timestamp
        msg1.getSequenceNumber() == 0L
        msg1.previousMessageRef == null

        msg2.getTimestamp() == timestamp + 100
        msg2.getSequenceNumber() == 0L
        msg2.previousMessageRef.timestamp == timestamp
        msg2.previousMessageRef.sequenceNumber == 0L

        msg3.getTimestamp() == timestamp + 200
        msg3.getSequenceNumber() == 0L
        msg3.previousMessageRef.timestamp == timestamp + 100
        msg3.previousMessageRef.sequenceNumber == 0L
    }

    void "publish with increasing sequence numbers"() {
        Date timestamp = new Date()
        when:
        StreamMessageV30 msg1 = (StreamMessageV30) msgCreationUtil.createStreamMessage(stream, [foo: "bar"], timestamp, null)
        StreamMessageV30 msg2 = (StreamMessageV30) msgCreationUtil.createStreamMessage(stream, [foo: "bar"], timestamp, null)
        StreamMessageV30 msg3 = (StreamMessageV30) msgCreationUtil.createStreamMessage(stream, [foo: "bar"], timestamp, null)
        then:
        assert msg1.getTimestamp() == timestamp.getTime()
        assert msg1.getSequenceNumber() == 0L
        assert msg1.previousMessageRef == null

        assert msg2.getTimestamp() == timestamp.getTime()
        assert msg2.getSequenceNumber() == 1L
        assert msg2.previousMessageRef.timestamp == timestamp.getTime()
        assert msg2.previousMessageRef.sequenceNumber == 0L

        assert msg3.getTimestamp() == timestamp.getTime()
        assert msg3.getSequenceNumber() == 2L
        assert msg3.previousMessageRef.timestamp == timestamp.getTime()
        assert msg3.previousMessageRef.sequenceNumber == 1L
    }

    void "publish with sequence numbers equal to 0 (same timestamp but different partitions)"() {
        Date timestamp = new Date()
        stream.partitions = 10
        when:
        StreamMessageV30 msg1 = (StreamMessageV30) msgCreationUtil.createStreamMessage(stream, [foo: "bar"], timestamp, null)
        StreamMessageV30 msg2 = (StreamMessageV30) msgCreationUtil.createStreamMessage(stream, [foo: "bar"], timestamp, "partition-key-1")
        StreamMessageV30 msg3 = (StreamMessageV30) msgCreationUtil.createStreamMessage(stream, [foo: "bar"], timestamp, "partition-key-2")
        then:
        assert msg1.getTimestamp() == timestamp.getTime()
        assert msg1.getSequenceNumber() == 0L
        assert msg1.previousMessageRef == null

        assert msg2.getTimestamp() == timestamp.getTime()
        assert msg2.getSequenceNumber() == 0L
        assert msg2.previousMessageRef == null

        assert msg2.getStreamPartition() != msg3.getStreamPartition()

        assert msg3.getTimestamp() == timestamp.getTime()
        assert msg3.getSequenceNumber() == 0L
        assert msg3.previousMessageRef == null
    }

    void "partitioner behaves correctly"() {
        when:
        stream.partitions = 10
        int[] partitions = [6, 7, 4, 4, 9, 1, 8, 0, 6, 6, 7, 6, 7, 3, 2, 2, 0, 9, 4, 9, 9, 5, 5, 1, 7, 3,
                            0, 6, 5, 6, 3, 6, 3, 5, 6, 2, 3, 6, 7, 2, 1, 3, 2, 7, 1, 1, 5, 1, 4, 0, 1, 9,
                            7, 4, 2, 3, 2, 9, 7, 7, 4, 3, 5, 4, 5, 3, 9, 0, 4, 8, 1, 7, 4, 8, 1, 2, 9, 9,
                            5, 3, 5, 0, 9, 4, 3, 9, 6, 7, 8, 6, 4, 6, 0, 1, 1, 5, 8, 3, 9, 7]
        then:
        for (int i = 0; i < 100; i++) {
            StreamMessageV30 msg = (StreamMessageV30) msgCreationUtil.createStreamMessage(stream, [foo: "bar"], new Date(), "key-"+i)
            assert msg.streamPartition == partitions[i]
        }
    }
}
