package com.streamr.client

import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.ResendLastOption
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import com.streamr.client.protocol.control_layer.BroadcastMessage
import com.streamr.client.protocol.control_layer.ResendLastRequest
import com.streamr.client.protocol.control_layer.ResendRangeRequest
import com.streamr.client.protocol.control_layer.ResendResponseResent
import com.streamr.client.protocol.control_layer.SubscribeRequest
import com.streamr.client.protocol.control_layer.SubscribeResponse
import com.streamr.client.protocol.control_layer.UnicastMessage
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV30
import com.streamr.client.rest.Stream
import spock.lang.Specification

class StreamrClientSpec extends Specification {

    private static TestWebSocketServer server = new TestWebSocketServer("localhost", 6000)

    TestingStreamrClient client
    int gapFillTimeout = 500
    int retryResendAfter = 500

    void setupSpec() {
        server.start()
    }

    void cleanupSpec() {
        server.stop()
    }

    void setup() {
        server.clear()
        SigningOptions signingOptions = new SigningOptions(SigningOptions.SignatureComputationPolicy.NEVER, SigningOptions.SignatureVerificationPolicy.NEVER)
        StreamrClientOptions options = new StreamrClientOptions(null, signingOptions, EncryptionOptions.getDefault(), server.getWsUrl(), "", gapFillTimeout, retryResendAfter)
        client = new TestingStreamrClient(options)
    }

    StreamMessageV30 createMsg(String streamId, long timestamp, long sequenceNumber, Long prevTimestamp, Long prevSequenceNumber) {
        MessageID msgId = new MessageID(streamId, 0, timestamp, sequenceNumber, "", "")
        MessageRef prev = prevTimestamp == null ? null : new MessageRef(prevTimestamp, prevSequenceNumber)
        return new StreamMessageV30(msgId, prev, StreamMessage.ContentType.CONTENT_TYPE_JSON, [hello: "world"], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
    }

    void "subscribe() sends SubscribeRequest and 1 ResendLastRequest after SubscribeResponse if answer received"() {
        Stream stream = new Stream("", "")
        stream.setId("test-stream")
        when:
        client.subscribe(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
            }
        }, new ResendLastOption(10))
        then:
        server.expect(new SubscribeRequest("test-stream", 0, null))
        when:
        client.receiveMessage(new SubscribeResponse("test-stream", 0))
        then:
        String subId = client.getSubId("test-stream", 0)
        server.expect(new ResendLastRequest("test-stream", 0, subId, 10, null))
        server.noOtherMessagesReceived()
        when:
        client.receiveMessage(new UnicastMessage(subId, createMsg("test-stream", 0, 0, null, null)))
        then:
        Thread.sleep(retryResendAfter + 200)
        server.noOtherMessagesReceived()
    }

    void "subscribe() sends 2 ResendLastRequest after SubscribeResponse if no answer received"() {
        Stream stream = new Stream("", "")
        stream.setId("test-stream")
        when:
        client.subscribe(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
            }
        }, new ResendLastOption(10))
        server.expect(new SubscribeRequest("test-stream", 0, null))
        client.receiveMessage(new SubscribeResponse("test-stream", 0))
        String subId = client.getSubId("test-stream", 0)
        then:
        Thread.sleep(retryResendAfter + 200)
        server.expect(new ResendLastRequest("test-stream", 0, subId, 10, null))
        server.expect(new ResendLastRequest("test-stream", 0, subId, 10, null))
        server.noOtherMessagesReceived()
    }

    void "requests a single resend if gap is detected and then filled"() {
        Stream stream = new Stream("", "")
        stream.setId("test-stream")
        when:
        client.subscribe(stream, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
            }
        })
        server.expect(new SubscribeRequest("test-stream", 0, null))
        client.receiveMessage(new SubscribeResponse("test-stream", 0))
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 0, 0, null, null)))
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 2, 0, 1, 0)))
        String subId = client.getSubId("test-stream", 0)
        then:
        server.expect(new ResendRangeRequest("test-stream", 0, subId, new MessageRef(0, 1), new MessageRef(1, 0), "", "", null))
        when:
        client.receiveMessage(new UnicastMessage(subId, createMsg("test-stream", 1, 0, 0, 0)))
        client.receiveMessage(new ResendResponseResent("test-stream", 0, subId))
        then:
        Thread.sleep(gapFillTimeout + 200)
        server.noOtherMessagesReceived()
    }

    void "requests multiple resends if gap is detected and not filled"() {
        Stream stream = new Stream("", "")
        stream.setId("test-stream")
        when:
        client.subscribe(stream, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
            }
        })
        server.expect(new SubscribeRequest("test-stream", 0, null))
        client.receiveMessage(new SubscribeResponse("test-stream", 0))
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 0, 0, null, null)))
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 2, 0, 1, 0)))
        String subId = client.getSubId("test-stream", 0)
        then:
        Thread.sleep(gapFillTimeout + 200)
        server.expect(new ResendRangeRequest("test-stream", 0, subId, new MessageRef(0, 1), new MessageRef(1, 0), "", "", null))
        server.expect(new ResendRangeRequest("test-stream", 0, subId, new MessageRef(0, 1), new MessageRef(1, 0), "", "", null))
    }
}
