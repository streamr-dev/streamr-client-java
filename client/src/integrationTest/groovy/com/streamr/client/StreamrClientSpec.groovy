package com.streamr.client

import com.streamr.client.options.ResendLastOption
import com.streamr.client.options.ResendOption
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.protocol.control_layer.BroadcastMessage
import com.streamr.client.protocol.control_layer.ErrorResponse
import com.streamr.client.protocol.control_layer.PublishRequest
import com.streamr.client.protocol.control_layer.ResendLastRequest
import com.streamr.client.protocol.control_layer.ResendRangeRequest
import com.streamr.client.protocol.control_layer.ResendResponseResent
import com.streamr.client.protocol.control_layer.SubscribeRequest
import com.streamr.client.protocol.control_layer.SubscribeResponse
import com.streamr.client.protocol.control_layer.UnicastMessage
import com.streamr.client.protocol.message_layer.MessageId
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.rest.ResourceNotFoundException
import com.streamr.client.rest.Stream
import com.streamr.client.rest.StreamrRestClient
import com.streamr.client.stream.EncryptionUtil
import com.streamr.client.stream.GroupKey
import com.streamr.client.stream.KeyExchangeUtil
import com.streamr.client.subs.Subscription
import com.streamr.client.testing.TestWebSocketServer
import com.streamr.client.testing.TestingAddresses
import com.streamr.client.testing.TestingContent
import com.streamr.client.testing.TestingMeta
import com.streamr.client.testing.TestingStreamrClient
import com.streamr.ethereum.common.Address
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class StreamrClientSpec extends Specification {
    @Shared
    private TestWebSocketServer server = new TestWebSocketServer("localhost", 6000)
    @Shared
    private Stream stream

    TestingStreamrClient client
    int gapFillTimeout = 500
    int retryResendAfter = 500

    void setupSpec() {
        server.start()

        stream = new Stream.Builder()
                .withName("")
                .withDescription("")
                .withId("test-stream")
                .withPartitions(1)
                .withRequireSignedData(false)
                .withRequireEncryptedData(false)
                .createStream()
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
        final BigInteger privateKey = new BigInteger("d462a6f2ccd995a346a841d110e8c6954930a1c22851c0032d3116d8ccd2296a", 16)
        // Turn off autoRevoke, otherwise it will try and to REST API calls
        StreamrClientOptions options = new StreamrClientOptions(
                new SigningOptions(SigningOptions.SignatureVerificationPolicy.AUTO),
                server.getWsUrl(),
                gapFillTimeout,
                retryResendAfter,
                false)
        options.reconnectRetryInterval = 1000
        options.connectionTimeoutMillis = 1000

        StreamrRestClient restClient = new StreamrRestClient.Builder()
				.withRestApiUrl(TestingMeta.REST_URL)
				.withPrivateKey(privateKey)
				.createStreamrRestClient();
        client = new TestingStreamrClient(options, restClient) {
            @Override
            public Stream getStream(String streamId) throws IOException, ResourceNotFoundException {
                return new Stream.Builder()
                        .withName("default mock stream from TestingStreamrClient")
                        .withDescription("")
                        .withId(streamId)
                        .withRequireSignedData(false)
                        .withRequireEncryptedData(false)
                        .createStream();
            }
        }
        client.connect()
        // TODO: client.login(privateKey)
        client.getSessionToken()

        expect:
        // The client subscribes to key exchange stream on connect
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 1
        }
        server.receivedControlMessages[0].message instanceof SubscribeRequest

        cleanup:
        // Remove that SubscribeRequest so that it doesn't need to be considered in each test case
        server.clear()
    }

    void cleanup() {
        client.disconnect()
    }

    StreamMessage createMsg(String streamId, long timestamp, long sequenceNumber, Long prevTimestamp, Long prevSequenceNumber) {
        MessageId msgId = new MessageId.Builder()
                .withStreamId(streamId)
                .withStreamPartition(0)
                .withTimestamp(timestamp)
                .withSequenceNumber(sequenceNumber)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        MessageRef prev = prevTimestamp == null ? null : new MessageRef(prevTimestamp, prevSequenceNumber)
        def map = [hello: "world"]
        return new StreamMessage.Builder()
                .withMessageId(msgId)
                .withPreviousMessageRef(prev)
                .withContent(TestingContent.fromJsonMap(map))
                .createStreamMessage()
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
        server.expect(new ResendRangeRequest(server.receivedControlMessages[1].message.requestId, stream.id, 0, new MessageRef(0, 1), new MessageRef(1, 0), TestingAddresses.PUBLISHER_ID, "msgChainId", client.sessionToken))

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
        server.expect(new ResendRangeRequest(server.receivedControlMessages[1].message.requestId, stream.id, 0, new MessageRef(0, 1), new MessageRef(1, 0), TestingAddresses.PUBLISHER_ID, "msgChainId", client.sessionToken))
        server.expect(new ResendRangeRequest(server.receivedControlMessages[2].message.requestId, stream.id, 0, new MessageRef(0, 1), new MessageRef(1, 0), TestingAddresses.PUBLISHER_ID, "msgChainId", client.sessionToken))
    }

    void "publish() publishes with the latest key added to keyStore"() {
        GroupKey groupKey = GroupKey.generate()
        client.getKeyStore().add(stream.getId(), groupKey)

        when:
        client.publish(stream, [test: "secret"])

        then:
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 1
        }
        ((PublishRequest) server.receivedControlMessages[0].message).streamMessage.getGroupKeyId() == groupKey.groupKeyId
        !((PublishRequest) server.receivedControlMessages[0].message).streamMessage.getSerializedContent().contains("secret")
    }

    void "publish() publishes with the key given as argument"() {
        GroupKey groupKey = GroupKey.generate()

        when:
        client.publish(stream, [test: "secret"], new Date(), null, groupKey)

        then:
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 1
        }
        ((PublishRequest) server.receivedControlMessages[0].message).streamMessage.getGroupKeyId() == groupKey.groupKeyId
        !((PublishRequest) server.receivedControlMessages[0].message).streamMessage.getSerializedContent().contains("secret")

        when:
        // The group key is not given in the next call
        client.publish(stream, [test: "another"])

        then:
        // The message is still encrypted with the current key
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 2
        }
        ((PublishRequest) server.receivedControlMessages[1].message).streamMessage.getGroupKeyId() == groupKey.groupKeyId
        !((PublishRequest) server.receivedControlMessages[1].message).streamMessage.getSerializedContent().contains("another")

    }

    void "publish() called with the current GroupKey does not rotate the key"() {
        GroupKey groupKey = GroupKey.generate()
        client.getKeyStore().add(stream.getId(), groupKey)

        when:
        client.publish(stream, [test: "secret"], new Date(), null, groupKey)

        then:
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 1
        }
        ((PublishRequest) server.receivedControlMessages[0].message).streamMessage.getGroupKeyId() == groupKey.groupKeyId
    }

    void "publish() called with a new GroupKey rotates the key"() {
        GroupKey currentKey = GroupKey.generate()
        GroupKey newKey = GroupKey.generate()
        client.getKeyStore().add(stream.getId(), currentKey)

        when:
        client.publish(stream, [test: "secret"], new Date(), null, newKey)

        then:
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 1
        }
        StreamMessage msg = ((PublishRequest) server.receivedControlMessages[0].message).streamMessage
        // content is encrypted with current key
        msg.getGroupKeyId() == currentKey.groupKeyId
        !msg.getSerializedContent().contains("secret")
        // new key is encrypted with current key
        msg.getNewGroupKey().getGroupKeyId() == newKey.getGroupKeyId()
        EncryptionUtil.decryptGroupKey(msg.getNewGroupKey(), currentKey) == newKey

        when:
        // next message is published
        client.publish(stream, [test: "secret"])

        then:
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 2
        }
        // content is encrypted with new key
        StreamMessage msg2 = ((PublishRequest) server.receivedControlMessages[1].message).streamMessage
        // content is encrypted with current key
        msg2.getGroupKeyId() == newKey.groupKeyId
        !msg2.getSerializedContent().contains("secret")
        // there is no new key attached this time
        msg2.getNewGroupKey() == null
    }

    void "client reconnects while publishing if server is temporarily down"() {
        Thread serverRestart = new Thread() {
            void run() {
                server.stop()
                server = new TestWebSocketServer("localhost", 6000)
                server.start()
            }
        }

        when:
        client.publish(stream, ["test": 1])
        client.publish(stream, ["test": 2])

        then:
        new PollingConditions().eventually {
            server.receivedControlMessages.size() == 2
        }

        when:
        serverRestart.start()
        Thread.sleep(200)
        client.publish(stream, ["test": 3])
        client.publish(stream, ["test": 4])
        client.publish(stream, ["test": 5])
        client.publish(stream, ["test": 6])

        then:
        new PollingConditions().eventually {
            // The client sends subscribe requests to key exchange stream on reconnect,
            // filter those out to only get the publish requests
            server.receivedControlMessages
                    .findAll { it.message instanceof PublishRequest }
                    .size() == 4
        }
    }

    void "calling publish from multiple threads during a server disconnect does not cause errors (CORE-1912)"() {
        Thread serverRestart = new Thread() {
            void run() {
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
        sleep(2000)
        threads.each { it.start() }

        then:
        new PollingConditions().within(60) {
            threads.find { it.alive } == null
        }
        errors == []
    }

    void "subscribed client reconnects if server is temporarily down"() {
        Thread serverRestart = new Thread() {
            void run() {
                server.stop()
                server = new TestWebSocketServer("localhost", 6000)
                server.start()
            }
        }
        subscribeClient()

        when:
        server.broadcastMessageToAll(stream, Collections.singletonMap("key", "msg #1"))

        then:
        new PollingConditions().eventually {
            client.getReceivedStreamMessages().size() == 1
        }

        when:
        serverRestart.start()
        sleep(2000)

        then:
        // Client should have resubscribed to its key exchange stream and the actual stream
        server.expect(new SubscribeRequest(server.receivedControlMessages[0].message.requestId, KeyExchangeUtil.getKeyExchangeStreamId(new Address("0x6807295093ac5da6fb2a10f7dedc5edd620804fb")), 0, client.sessionToken))
        server.expect(new SubscribeRequest(server.receivedControlMessages[1].message.requestId, stream.id, 0, client.sessionToken))

        when:
        server.respondTo(server.receivedControlMessages[1])
        server.broadcastMessageToAll(stream, Collections.singletonMap("key", "msg #2"))

        then:
        new PollingConditions().eventually {
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
