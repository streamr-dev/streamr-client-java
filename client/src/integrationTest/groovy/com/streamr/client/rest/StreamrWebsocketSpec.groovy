package com.streamr.client.rest

import com.streamr.client.MessageHandler
import com.streamr.client.StreamrClient
import com.streamr.client.options.ResendFromOption
import com.streamr.client.options.ResendLastOption
import com.streamr.client.options.ResendRangeOption
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.subs.Subscription
import com.streamr.client.testing.TestingKeys
import com.streamr.client.testing.TestingStreamrClient
import com.streamr.client.testing.TestingStreams
import com.streamr.client.stream.GroupKey
import com.streamr.client.utils.UnableToDecryptException
import org.java_websocket.enums.ReadyState
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class StreamrWebsocketSpec extends Specification {
    private BigInteger publisherPrivateKey
    private BigInteger subscriberPrivateKey
    private StreamrClient publisher
    private StreamrClient subscriber
    private Stream stream
    PollingConditions within10sec = new PollingConditions(timeout: 10)

    void setup() {
        publisherPrivateKey = TestingKeys.generatePrivateKey()
        subscriberPrivateKey = TestingKeys.generatePrivateKey()
        publisher = TestingStreamrClient.createClientWithPrivateKey(publisherPrivateKey)
        subscriber = TestingStreamrClient.createClientWithPrivateKey(publisherPrivateKey)

        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("")
                .withRequireEncryptedData(false)
                .withRequireSignedData(false)
                .createStream()
        this.stream = publisher.createStream(proto)
        publisher.grant(this.stream, Permission.Operation.stream_get, subscriber.getPublisherId().toString())
        publisher.grant(this.stream, Permission.Operation.stream_subscribe, subscriber.getPublisherId().toString())
    }

    void cleanup() {
        if (publisher != null && publisher.state != ReadyState.CLOSED) {
            publisher.disconnect()
        }
        if (subscriber != null && subscriber.state != ReadyState.CLOSED) {
            subscriber.disconnect()
        }
    }

    void "client can connect and disconnect over websocket"() {
        when:
        publisher.connect()

        then:
        publisher.state == ReadyState.OPEN

        when:
        publisher.disconnect()

        then:
        publisher.state == ReadyState.CLOSED
    }


    void "client automatically connects for publishing"() {
        Stream stream = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("")
                .createStream()
        stream = publisher.createStream(stream)

        when:
        publisher.publish(stream, [foo: "bar"], new Date())

        then:
        publisher.state == ReadyState.OPEN
    }

    void "client automatically connects for subscribing"() {
        Stream stream = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("")
                .createStream()
        stream = subscriber.createStream(stream)

        when:
        subscriber.subscribe(stream, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {}
        })

        then:
        subscriber.state == ReadyState.OPEN
    }

    void "a subscriber receives published messages"() {
        int msgCount = 0
        Subscription sub

        when:
        // Subscribe to the stream
        StreamMessage latestMsg
        sub = subscriber.subscribe(stream, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                msgCount++
                latestMsg = message
            }

        })
        Thread.sleep(2000)

        // Produce messages to the stream
        for (int i = 1; i <= 10; i++) {
            publisher.publish(stream, [i: i])
            Thread.sleep(200)
        }

        then:
        // All messages have been received by subscriber
        within10sec.eventually {
            msgCount == 10
        }
        latestMsg.parsedContent.i == 10

        when:
        subscriber.unsubscribe(sub)

        then:
        within10sec.eventually {
            !sub.isSubscribed()
        }

        when:
        publisher.publish(stream, [i: 11])

        then:
        // No more messages should be received, since we're unsubscribed
        msgCount == 10
    }

    void "subscriber receives signed message if published with signature"() {
        when:
        // Subscribe to the stream
        StreamMessage msg
        subscriber.subscribe(stream, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                //reaching this point ensures that the signature verification didn't throw
                msg = (StreamMessage) message
            }
        })

        Thread.sleep(2000)

        publisher.publish(stream, [test: 'signed'])

        then:
        within10sec.eventually {
            msg != null
        }
        msg.getPublisherId() == publisher.getPublisherId()
        msg.signatureType == StreamMessage.SignatureType.ETH
        msg.signature != null
    }

    void "subscriber can decrypt messages when he knows the keys used to encrypt"() {
        GroupKey key = GroupKey.generate()
        subscriber.getKeyStore().add(stream.getId(), key)

        when:
        // Subscribe to the stream
        StreamMessage msg
        subscriber.subscribe(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                //reaching this point ensures that the signature verification and decryption didn't throw
                msg = (StreamMessage) message
            }
        }, null)
        Thread.sleep(2000)

        publisher.publish(stream, [test: 'clear text'], new Date(), null, key)

        then:
        within10sec.eventually {
            msg != null && msg.getParsedContent() == [test: 'clear text']
        }

        when:
        // publishing a second message with a new group key, triggers key rotate & announce
        publisher.publish(stream, [test: 'another clear text'], new Date(), null, GroupKey.generate())

        then:
        // no need to explicitly give the new group key to the subscriber
        within10sec.eventually {
            msg.getParsedContent() == [test: 'another clear text']
        }
    }

    void "subscriber can get the group key and decrypt encrypted messages using an RSA key pair"() {
        GroupKey key = GroupKey.generate()

        when:
        // Subscribe to the stream without knowing the group key
        StreamMessage msg1 = null
        StreamMessage msg2 = null
        subscriber.subscribe(stream, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                //reaching this point ensures that the signature verification and decryption didn't throw
                if (msg1 == null) {
                    msg1 = message
                } else {
                    msg2 = message
                }
            }
        })
        Thread.sleep(2000)

        publisher.publish(stream, [test: 'clear text'], new Date(), null, key)

        then:
        within10sec.eventually {
            // the subscriber got the group key and can decrypt
            msg1 != null && msg1.getParsedContent() == [test: 'clear text']
        }

        when:
        // publishing a second message with a new group key
        publisher.publish(stream, [test: 'another clear text'], new Date(), null, GroupKey.generate())

        then:
        within10sec.eventually {
            // no need to explicitly give the new group key to the subscriber
            msg2 != null && msg2.getParsedContent() == [test: 'another clear text']
        }
    }

    void "subscriber can get the new group key after reset and decrypt encrypted messages"() {
        GroupKey key = GroupKey.generate()

        when:
        // Subscribe to the stream without knowing the group key
        StreamMessage msg1 = null
        StreamMessage msg2 = null
        subscriber.subscribe(stream, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                //reaching this point ensures that the signature verification and decryption didn't throw
                if (msg1 == null) {
                    msg1 = message
                } else {
                    msg2 = message
                }
            }
        })
        Thread.sleep(2000)

        publisher.publish(stream, [test: 'clear text'], new Date(), null, key)

        then:
        within10sec.eventually {
            // the subscriber got the group key and can decrypt
            msg1 != null && msg1.getParsedContent() == [test: 'clear text']
        }

        when:
        // publishing a second message after a rekey to revoke old subscribers
        publisher.rekey(stream)
        publisher.publish(stream, [test: 'another clear text'])

        then:
        within10sec.eventually {
            // no need to explicitly give the new group key to the subscriber
            msg2 != null && msg2.getParsedContent() == [test: 'another clear text']
        }
    }

    void "subscriber can get the historical keys and decrypt old encrypted messages using an RSA key pair"() {
        // publishing historical messages with different group keys before subscribing
        List<GroupKey> keys = [GroupKey.generate(), GroupKey.generate()]
        publisher.publish(stream, [test: 'clear text'], new Date(), null, keys[0])
        publisher.publish(stream, [test: 'another clear text'], new Date(), null, keys[1])
        Thread.sleep(3000)

        when:
        // Subscribe to the stream with resend last without knowing the group keys
        StreamMessage msg1 = null
        StreamMessage msg2 = null
        StreamMessage msg3 = null
        subscriber.subscribe(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                //reaching this point ensures that the signature verification and decryption didn't throw
                if (msg1 == null) {
                    msg1 = message
                } else if (msg2 == null) {
                    msg2 = message
                } else if (msg3 == null) {
                    msg3 = message
                } else {
                    throw new RuntimeException("Received unexpected message: " + message.serialize())
                }

            }
        }, new ResendLastOption(3)) // need to resend 3 messages because the announce counts

        then:
        within10sec.eventually {
            msg1 != null && msg2 != null
        }
        // the subscriber got the group keys and can decrypt the old messages
        msg1.getParsedContent() == [test: 'clear text']
        msg2.getParsedContent() == [test: 'another clear text']

        when:
        // The publisher publishes another message with latest key
        publisher.publish(stream, [test: '3'], new Date(), null, keys[1])

        then:
        within10sec.eventually {
            msg3 != null
        }
        msg3.getParsedContent() == [test: '3']
    }

    void "subscribe with resend last"() {
        boolean received = false

        when:
        publisher.publish(stream, [i: 1])
        Thread.sleep(6000) // wait to land in storage
        // Subscribe to the stream
        subscriber.subscribe(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                received = message.getParsedContent() == [i: 1]
            }
        }, new ResendLastOption(1))

        Thread.sleep(6000)

        then:
        received
    }

    void "subscribe with resend from"() {
        boolean received = false
        boolean done = false

        when:
        publisher.publish(stream, [i: 1])
        Thread.sleep(2000)

        // Subscribe to the stream
        subscriber.subscribe(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                received = message.getParsedContent() == [i: 1]
            }

            void done(Subscription sub) {
                done = true
            }
        }, new ResendFromOption(new Date(0)))

        then:
        within10sec.eventually() {
            done && received
        }
    }

    void "resend last"() {
        List receivedMsg = []
        boolean done = false

        when:
        for (int i = 0; i <= 10; i++) {
            publisher.publish(stream, [i: i])
        }
        Thread.sleep(6000) // wait to land in storage

        // Resend last
        subscriber.resend(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                receivedMsg.add(message.getParsedContent())
            }

            void done(Subscription sub) {
                done = true
            }
        }, new ResendLastOption(5))

        then:
        List expectedMessages = [
                Collections.singletonMap("i", 6.0),
                Collections.singletonMap("i", 7.0),
                Collections.singletonMap("i", 8.0),
                Collections.singletonMap("i", 9.0),
                Collections.singletonMap("i", 10.0)
        ]

        within10sec.eventually() {
            done && receivedMsg == expectedMessages
        }
    }

    void "resend from"() {
        List receivedMsg = []
        boolean done = false
        Date resendFromDate

        when:
        for (int i = 0; i <= 10; i++) {
            publisher.publish(stream, [i: i])

            if (i == 7) {
                resendFromDate = new Date()
            }
        }
        Thread.sleep(6000) // wait to land in storage

        // Resend from
        subscriber.resend(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                receivedMsg.add(message.getParsedContent())
            }

            void done(Subscription sub) {
                done = true
            }
        }, new ResendFromOption(resendFromDate))

        then:
        List expectedMessages = [
                Collections.singletonMap("i", 8.0),
                Collections.singletonMap("i", 9.0),
                Collections.singletonMap("i", 10.0)
        ]

        within10sec.eventually() {
            done && receivedMsg == expectedMessages
        }
    }

    void "resend range"() {
        List receivedMsg = []
        boolean done = false
        Date resendFromDate
        Date resendToDate

        when:
        for (int i = 0; i <= 10; i++) {
            Date date = new Date()
            publisher.publish(stream, [i: i], date)

            if (i == 3) {
                resendFromDate = new Date(date.getTime() + 1)
            }

            if (i == 7) {
                resendToDate = new Date(date.getTime() - 1)
            }
        }
        Thread.sleep(6000) // wait to land in storage

        // Resend range
        subscriber.resend(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                receivedMsg.add(message.getParsedContent())
            }

            void done(Subscription sub) {
                done = true
            }
        }, new ResendRangeOption(resendFromDate, resendToDate))

        then:
        List expectedMessages = [
                Collections.singletonMap("i", 4.0),
                Collections.singletonMap("i", 5.0),
                Collections.singletonMap("i", 6.0)
        ]

        within10sec.eventually() {
            done && receivedMsg == expectedMessages
        }
    }

    void "resend range again"() {
        List receivedMsg = []
        boolean done = false
        int j = 0
        Date resendFromDate
        Date resendToDate

        when:
        for (j = 0; j < 2; j++) {
            for (int i = 0; i <= 10; i++) {
                Date date = new Date()
                publisher.publish(stream, [i: i], date)

                if (i == 3) {
                    resendFromDate = new Date(date.getTime() + 1)
                }

                if (i == 7) {
                    resendToDate = new Date(date.getTime() - 1)
                }
            }
            Thread.sleep(6000) // wait to land in storage

            // Resend range
            subscriber.resend(stream, 0, new MessageHandler() {
                @Override
                void onMessage(Subscription s, StreamMessage message) {
                    receivedMsg.add(message.getParsedContent())
                }

                void done(Subscription sub) {
                    if (j == 1)
                        done = true
                }
            }, new ResendRangeOption(resendFromDate, resendToDate))
        }
        then:
        List expectedMessages = [
                Collections.singletonMap("i", 4.0),
                Collections.singletonMap("i", 5.0),
                Collections.singletonMap("i", 6.0),
                Collections.singletonMap("i", 4.0),
                Collections.singletonMap("i", 5.0),
                Collections.singletonMap("i", 6.0)
        ]

        within10sec.eventually() {
            done && receivedMsg == expectedMessages
        }
    }

    void "subscribe with resend last, with key exchange"() {
        boolean stop = false
        int publishedMessages = 0
        int receivedMessages = 0
        Thread publisherThread = Thread.start {
            int i = 0
            while (!stop) {
                // The publisher generates a new key for every message
                publishedMessages++
                publisher.publish(stream, [i: i++], new Date(), "", GroupKey.generate())
                Thread.sleep(500)
            }
        }
        Thread.sleep(5000) // make sure some published messages have time to get written to storage

        when:
        // Subscribe with resend last
        subscriber.subscribe(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                receivedMessages++
            }
        }, new ResendLastOption(1000)) // resend all previous messages to make the counters match
        Thread.sleep(3000) // Time to do the key exchanges etc.
        stop = true

        then:
        within10sec.eventually {
            !publisherThread.isAlive()
        }

        then:
        within10sec.eventually {
            publishedMessages == receivedMessages
        }
    }

    void "two instances of same publisher publishing to the same stream"() {
        boolean stop = false
        StreamrClient publisher2 = TestingStreamrClient.createClientWithPrivateKey(publisherPrivateKey) // same private key
        publisher.grant(stream, Permission.Operation.stream_get, publisher2.getPublisherId().toString())
        publisher.grant(stream, Permission.Operation.stream_publish, publisher2.getPublisherId().toString())

        GroupKey keyPublisher1 = GroupKey.generate()
        GroupKey keyPublisher2 = GroupKey.generate()
        int publishedByPublisher1 = 0
        int publishedByPublisher2 = 0
        int receivedFromPublisher1 = 0
        int receivedFromPublisher2 = 0
        int unableToDecryptCount = 0

        Thread publisher1Thread = Thread.start {
            int i = 0
            while (!stop) {
                // The publisher generates a new key for every message
                publishedByPublisher1++
                publisher.publish(stream, [publisher: 1, i: i++], new Date(), "", keyPublisher1)
                Thread.sleep(500)
            }
        }
        Thread publisher2Thread = Thread.start {
            int i = 0
            while (!stop) {
                // The publisher generates a new key for every message
                publishedByPublisher2++
                publisher2.publish(stream, [publisher: 2, i: i++], new Date(), "", keyPublisher2)
                Thread.sleep(500)
            }
        }

        Thread.sleep(5000) // make sure some published messages have time to get written to storage

        when:
        // Subscribe with resend last
        subscriber.subscribe(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                if (message.getParsedContent().publisher == 1) {
                    receivedFromPublisher1++
                    log.info("Received from publisher1 message: {}", message.getParsedContent())
                } else if (message.getParsedContent().publisher == 2) {
                    receivedFromPublisher2++
                    log.info("Received from publisher2 message: {}", message.getParsedContent())
                } else {
                    throw new RuntimeException("Received an unexpected message: " + message.getParsedContent())
                }
            }

            @Override
            void onUnableToDecrypt(UnableToDecryptException e) {
                unableToDecryptCount++
            }
        }, new ResendLastOption(1000)) // resend all previous messages to make the counters match
        Thread.sleep(3000) // Time to do the key exchanges etc.
        stop = true

        then:
        within10sec.eventually {
            !publisher1Thread.isAlive() && !publisher2Thread.isAlive()
        }

        then:
        within10sec.eventually {
            publishedByPublisher1 == receivedFromPublisher1 && publishedByPublisher2 == receivedFromPublisher2
        }
        unableToDecryptCount == 0

        cleanup:
        publisher2.disconnect()
    }
}
