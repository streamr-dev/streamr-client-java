package com.streamr.client

import com.streamr.client.authentication.ApiKeyAuthenticationMethod
import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.ResendLastOption
import com.streamr.client.options.ResendOption
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import com.streamr.client.protocol.StreamrSpecification
import com.streamr.client.protocol.control_layer.*
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.rest.Stream
import com.streamr.client.subs.Subscription
import com.streamr.client.utils.GroupKeyStore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class StreamrClientSpec extends StreamrSpecification {

    private static TestWebSocketServer server = new TestWebSocketServer("localhost", 6000)

    TestingStreamrClient client
    int gapFillTimeout = 500
    int retryResendAfter = 500

    static Stream stream

    void setupSpec() {
        server.start()

        stream = new Stream("", "")
        stream.setId("test-stream")
        stream.setPartitions(1)
    }

    void cleanupSpec() {
        server.stop()
    }

    void subscribeClient(ResendOption resendOption = null) {
        Subscription sub = client.subscribe(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {}
        }, resendOption)

        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 1
        }
        server.expect(new SubscribeRequest(server.receivedControlMessages[0].message.requestId, stream.id, 0, client.sessionToken))

        client.receiveMessage(new SubscribeResponse(server.receivedControlMessages[0].message.requestId, stream.id, 0))
        new PollingConditions().eventually {
            sub.isSubscribed()
        }
    }

    void setup() {
        server.clear()
        SigningOptions signingOptions = new SigningOptions(SigningOptions.SignatureComputationPolicy.NEVER, SigningOptions.SignatureVerificationPolicy.NEVER)

        GroupKeyStore keyStore = Mock(GroupKeyStore)
        EncryptionOptions encryptionOptions = new EncryptionOptions(keyStore, null, null, false)
        StreamrClientOptions options = new StreamrClientOptions(new ApiKeyAuthenticationMethod("apikey"), signingOptions, encryptionOptions, server.getWsUrl(), "", gapFillTimeout, retryResendAfter, false)
        options.reconnectRetryInterval = 1000
        options.connectionTimeoutMillis = 1000
        client = new TestingStreamrClient(options)
        client.connect()
    }

    void cleanup() {
        client.disconnect()
    }

    StreamMessage createMsg(String streamId, long timestamp, long sequenceNumber, Long prevTimestamp, Long prevSequenceNumber) {
        MessageID msgId = new MessageID(streamId, 0, timestamp, sequenceNumber, publisherId.toString(), "msgChainId")
        MessageRef prev = prevTimestamp == null ? null : new MessageRef(prevTimestamp, prevSequenceNumber)
        return new StreamMessage(msgId, prev, [hello: "world"])
    }

    void "subscribe() sends SubscribeRequest and 1 ResendLastRequest after SubscribeResponse if answer received"() {
        when:
        subscribeClient(new ResendLastOption(10))

        then:
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 2
        }
        server.expect(new ResendLastRequest(server.receivedControlMessages[1].message.requestId, stream.id, 0, 10, client.sessionToken))

        when:
        client.receiveMessage(new UnicastMessage(server.receivedControlMessages[1].message.requestId, createMsg("test-stream", 0, 0, null, null)))
        Thread.sleep(retryResendAfter + 200)

        then:
        server.noOtherMessagesReceived()
    }

    void "subscribe() sends 2 ResendLastRequest after SubscribeResponse if no answer received"() {
        when:
        subscribeClient(new ResendLastOption(10))
        Thread.sleep(retryResendAfter + 200)

        then:
        server.receivedControlMessages.size() == 3
        server.expect(new ResendLastRequest(server.receivedControlMessages[1].message.requestId, stream.id, 0, 10, client.sessionToken))
        server.expect(new ResendLastRequest(server.receivedControlMessages[2].message.requestId, stream.id, 0, 10, client.sessionToken))
    }

    void "requests a single resend if gap is detected and then filled"() {
        when:
        subscribeClient()
        client.receiveMessage(new BroadcastMessage("", createMsg("test-stream", 0, 0, null, null)))
        client.receiveMessage(new BroadcastMessage("", createMsg("test-stream", 2, 0, 1, 0)))
        Thread.sleep(gapFillTimeout)

        then:
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 2
        }
        server.expect(new ResendRangeRequest(server.receivedControlMessages[1].message.requestId, stream.id, 0, new MessageRef(0, 1), new MessageRef(1, 0), publisherId, "msgChainId", client.sessionToken))

        when:
        client.receiveMessage(new UnicastMessage(server.receivedControlMessages[1].message.requestId, createMsg("test-stream", 1, 0, 0, 0)))
        client.receiveMessage(new ResendResponseResent(server.receivedControlMessages[1].message.requestId, stream.id, 0))

        then:
        Thread.sleep(gapFillTimeout + 200)
        server.noOtherMessagesReceived()
    }

    void "requests multiple resends if gap is detected and not filled"() {
        when:
        subscribeClient()
        client.receiveMessage(new BroadcastMessage("", createMsg("test-stream", 0, 0, null, null)))
        client.receiveMessage(new BroadcastMessage("", createMsg("test-stream", 2, 0, 1, 0)))
        Thread.sleep(2 * gapFillTimeout + 200)

        then:
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 3
        }
        server.expect(new ResendRangeRequest(server.receivedControlMessages[1].message.requestId, stream.id, 0, new MessageRef(0, 1), new MessageRef(1, 0), publisherId, "msgChainId", client.sessionToken))
        server.expect(new ResendRangeRequest(server.receivedControlMessages[2].message.requestId, stream.id, 0, new MessageRef(0, 1), new MessageRef(1, 0), publisherId, "msgChainId", client.sessionToken))
    }

    void "client reconnects while publishing if server is temporarily down"() {
        Thread serverRestart = new Thread() {
            void run() {
                server.stop()
                server = new TestWebSocketServer("localhost", 6000)
                sleep(2000)
                server.start()
                sleep(2000)
            }
        }

        when:
        client.publish(stream, ["test": 1])
        Thread.sleep(200)
        client.publish(stream, ["test": 2])

        then:
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 2
        }

        when:
        serverRestart.start()
        Thread.sleep(200)
        client.publish(stream, ["test": 3])
        Thread.sleep(200)
        client.publish(stream, ["test": 4])
        Thread.sleep(200)
        client.publish(stream, ["test": 5])
        Thread.sleep(200)
        client.publish(stream, ["test": 6])

        then:
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 4 // count restarts on the server restart
        }
    }

    void "calling publish from multiple threads during a server disconnect does not cause errors (CORE-1912)"() {
        Thread serverRestart = new Thread() {
            void run(){
                server.stop()
                server = new TestWebSocketServer("localhost", 6000)
                server.start()
            }
        }

        List errors = Collections.synchronizedList([])
        List<Thread> threads = []
        for (int i = 4; i < 100; ++i) {
            threads.add(new Thread() {
                void run() {
                    try {
                        client.publish(stream, ["test": i])
                    } catch (e) {
                        errors.add(e)
                    }
                }
            })
        }

        when:
        client.publish(stream, ["test": 1])
        client.publish(stream, ["test": 2])
        client.publish(stream, ["test": 3])
        serverRestart.start()
        threads.each { it.start() }

        then:
        new PollingConditions().within(60) {
            threads.find { it.alive } == null
        }
        errors == []
    }

    void "subscribed client reconnects if server is temporarily down"() {
        subscribeClient()

        when:
        server.broadcastMessageToAll(stream, Collections.singletonMap("key", "msg #1"))

        then:
        new PollingConditions().eventually {
            client.getReceivedStreamMessages().size() == 1
        }

        when:
        server.stop()
        server = new TestWebSocketServer("localhost", 6000)
        sleep(4000)
        server.start()
        sleep(10000)

        then:
        // Client should have resubscribed
        server.expect(new SubscribeRequest(server.receivedControlMessages[0].message.requestId, stream.id, 0, client.sessionToken))

        when:
        server.respondTo(server.receivedControlMessages[0])
        server.broadcastMessageToAll(stream, Collections.singletonMap("key", "msg #2"))

        then:
        new PollingConditions().eventually {
            println "Checking received messages: ${client.getReceivedStreamMessages().size()}"
            client.getReceivedStreamMessages().size() == 2
        }
    }

    void "error message handler is called"() {
        boolean errorIsHandled = false
        client.setErrorMessageHandler({ ErrorResponse error ->
            errorIsHandled = true
        })
        ErrorResponse err = new ErrorResponse("requestId", "error occurred", "TEST_ERROR")

        when:
        client.handleMessage(err.toJson())

        then:
        errorIsHandled
    }
}
