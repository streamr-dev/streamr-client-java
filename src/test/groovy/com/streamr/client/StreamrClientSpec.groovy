package com.streamr.client

import com.streamr.client.authentication.ApiKeyAuthenticationMethod
import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.ResendLastOption
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import com.streamr.client.protocol.control_layer.BroadcastMessage
import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.PublishRequest
import com.streamr.client.protocol.control_layer.ResendLastRequest
import com.streamr.client.protocol.control_layer.ResendRangeRequest
import com.streamr.client.protocol.control_layer.ResendResponseResent
import com.streamr.client.protocol.control_layer.SubscribeRequest
import com.streamr.client.protocol.control_layer.SubscribeResponse
import com.streamr.client.protocol.control_layer.UnicastMessage
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.rest.Stream
import com.streamr.client.subs.Subscription
import spock.lang.Specification

import java.util.function.Function

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

        StreamrClientOptions options = new StreamrClientOptions(new ApiKeyAuthenticationMethod("apikey"), signingOptions, EncryptionOptions.getDefault(), server.getWsUrl(), "", gapFillTimeout, retryResendAfter)
        client = new TestingStreamrClient(options)
    }

    StreamMessageV31 createMsg(String streamId, long timestamp, long sequenceNumber, Long prevTimestamp, Long prevSequenceNumber) {
        MessageID msgId = new MessageID(streamId, 0, timestamp, sequenceNumber, "", "")
        MessageRef prev = prevTimestamp == null ? null : new MessageRef(prevTimestamp, prevSequenceNumber)
        return new StreamMessageV31(msgId, prev, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [hello: "world"], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
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
        server.expect(new SubscribeRequest("test-stream", 0, client.getSessionToken()))
        when:
        client.receiveMessage(new SubscribeResponse("test-stream", 0))
        then:
        String subId = client.getSubId("test-stream", 0)
        server.expect(new ResendLastRequest("test-stream", 0, subId, 10, client.getSessionToken()))
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
        server.expect(new SubscribeRequest("test-stream", 0, client.getSessionToken()))
        client.receiveMessage(new SubscribeResponse("test-stream", 0))
        String subId = client.getSubId("test-stream", 0)
        then:
        Thread.sleep(retryResendAfter + 200)
        server.expect(new ResendLastRequest("test-stream", 0, subId, 10, client.getSessionToken()))
        server.expect(new ResendLastRequest("test-stream", 0, subId, 10, client.getSessionToken()))
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
        server.expect(new SubscribeRequest("test-stream", 0, client.getSessionToken()))
        client.receiveMessage(new SubscribeResponse("test-stream", 0))
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 0, 0, null, null)))
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 2, 0, 1, 0)))
        String subId = client.getSubId("test-stream", 0)
        Thread.sleep(gapFillTimeout)
        then:
        server.expect(new ResendRangeRequest("test-stream", 0, subId, new MessageRef(0, 1), new MessageRef(1, 0), "", "", client.getSessionToken()))
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
        Subscription sub = client.subscribe(stream, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
            }
        })
        server.expect(new SubscribeRequest("test-stream", 0, client.getSessionToken()))
        client.receiveMessage(new SubscribeResponse("test-stream", 0))
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 0, 0, null, null)))
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 2, 0, 1, 0)))

        then:
        Thread.sleep(2 * gapFillTimeout + 200)
        server.expect(new ResendRangeRequest("test-stream", 0, sub.id, new MessageRef(0, 1), new MessageRef(1, 0), "", "", client.getSessionToken()))
        server.expect(new ResendRangeRequest("test-stream", 0, sub.id, new MessageRef(0, 1), new MessageRef(1, 0), "", "", client.getSessionToken()))
    }

    void "processes waiting messages from queue after gap is filled successfully"() {
        Stream stream = new Stream("", "")
        stream.setId("test-stream")
        List<StreamMessage> messages = []

        when:
        Subscription sub = client.subscribe(stream, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                println "Received message ${message.getMessageRef().getTimestamp()}"
                messages.add(message)
            }
        })
        then:
        server.expect(new SubscribeRequest("test-stream", 0, client.getSessionToken()))

        when:
        client.receiveMessage(new SubscribeResponse("test-stream", 0))
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 0, 0, null, null)))
        // This message should get queued and processed after successful gapfill
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 2, 0, 1, 0)))
        Thread.sleep(gapFillTimeout + 200)

        then:
        messages.last().getMessageRef().getTimestamp() == 0
        server.expect(new ResendRangeRequest("test-stream", 0, sub.id, new MessageRef(0, 1), new MessageRef(1, 0), "", "", client.getSessionToken()))

        when: "gap is filled"
        client.receiveMessage(new UnicastMessage(client.getSubId(stream.id, 0), createMsg("test-stream", 1, 0, 0, 0)))
        client.receiveMessage(new ResendResponseResent(stream.id, 0, client.getSubId(stream.id, 0)))

        then: "all messages are processed"
        messages.last().getMessageRef().getTimestamp() == 2
    }

    void "sends a new gapfill request after a gap is partially filled"() {
        Stream stream = new Stream("", "")
        stream.setId("test-stream")
        List<StreamMessage> messages = []

        when:
        client.subscribe(stream, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                println "Received message ${message.getMessageRef().getTimestamp()}"
                messages.add(message)
            }
        })
        then:
        server.expect(new SubscribeRequest("test-stream", 0, client.getSessionToken()))

        when: "gap is detected"
        client.receiveMessage(new SubscribeResponse("test-stream", 0))
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 0, 0, null, null)))
        // This message should get eventually processed
        client.receiveMessage(new BroadcastMessage(createMsg("test-stream", 3, 0, 2, 0)))
        Thread.sleep(gapFillTimeout + 200)

        then: "resend request is sent"
        messages.last().getMessageRef().getTimestamp() == 0
        server.expect(new ResendRangeRequest("test-stream", 0, client.getSubId(stream.id, 0), new MessageRef(0, 1), new MessageRef(2, 0), "", "", client.getSessionToken()))

        when: "half of the gap is filled (message 2 still missing)"
        client.receiveMessage(new UnicastMessage(client.getSubId(stream.id, 0), createMsg("test-stream", 1, 0, 0, 0)))
        client.receiveMessage(new ResendResponseResent(stream.id, 0, client.getSubId(stream.id, 0)))
        Thread.sleep(gapFillTimeout + 200)

        then: "a new resend should be sent after gapFillTimeout"
        server.expect(new ResendRangeRequest("test-stream", 0, client.getSubId(stream.id, 0), new MessageRef(1, 1), new MessageRef(2, 0), "", "", client.getSessionToken()))

        when: "the final missing message is received"
        client.receiveMessage(new UnicastMessage(client.getSubId(stream.id, 0), createMsg("test-stream", 2, 0, 1, 0)))
        client.receiveMessage(new ResendResponseResent(stream.id, 0, client.getSubId(stream.id, 0)))

        then: "the queued message is processed"
        messages.last().getMessageRef().getTimestamp() == 3
    }

    void "client reconnects while publishing if server is temporarily down"() {
        when:
        Stream stream = new Stream("", "")
        stream.setId("test-stream")
        stream.setPartitions(1)
        Thread serverRestart = new Thread(){
            void run(){
                server.stop()
                server = new TestWebSocketServer("localhost", 6000)
                sleep(2000)
                server.start()
            }
        }
        then:
        client.publish(stream, ["test": 1])
        Thread.sleep(200)
        client.publish(stream, ["test": 2])
        serverRestart.start()
        Thread.sleep(200)
        client.publish(stream, ["test": 3])
        Thread.sleep(200)
        client.publish(stream, ["test": 4])
        Thread.sleep(200)
        client.publish(stream, ["test": 5])
        Thread.sleep(200)
        client.publish(stream, ["test": 6])
    }
}
