package com.streamr.client

import com.streamr.client.authentication.ApiKeyAuthenticationMethod
import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.ResendLastOption
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import com.streamr.client.protocol.control_layer.*
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.rest.Stream
import com.streamr.client.subs.Subscription
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

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
        then:
        Thread.sleep(2 * gapFillTimeout + 200)
        server.expect(new ResendRangeRequest("test-stream", 0, subId, new MessageRef(0, 1), new MessageRef(1, 0), "", "", client.getSessionToken()))
        server.expect(new ResendRangeRequest("test-stream", 0, subId, new MessageRef(0, 1), new MessageRef(1, 0), "", "", client.getSessionToken()))
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

    void "subscribed client reconnects if server is temporarily down"() {
        when:
        client.options.reconnectRetryInterval = 100

        Stream stream = new Stream("", "")
        stream.setId("test-stream")
        stream.setPartitions(1)

        List<StreamMessage> receivedMessages = new ArrayList<>()
        client.subscribe(stream, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                receivedMessages.push(message)
            }
        })

        new Thread() {
            void run() {
                server.sendSubscribeToAll(stream.getId(), 0)
                server.sendToAll(stream, Collections.singletonMap("key", "msg #1"))
                sleep(250)
                server.stop()
                server = new TestWebSocketServer("localhost", 6000)
                sleep(250)
                server.start()
                sleep(500)
                server.sendSubscribeToAll(stream.getId(), 0)
                server.sendToAll(stream, Collections.singletonMap("key", "msg #2"))
            }
        }.start()

        PollingConditions conditions = new PollingConditions(timeout: 10, initialDelay: 1, factor: 1)

        then:
        conditions.eventually {
            receivedMessages.size() == 2
        }
    }

    void "error message handler is called"() {
        setup:
        boolean errorIsHandled = false
        when:
        client.setErrorMessageHandler({ ErrorResponse error ->
            errorIsHandled = true
        })
        ErrorResponse err = new ErrorResponse("error occured")
        client.handleMessage(err.toJson())
        then:
        errorIsHandled
    }
}
