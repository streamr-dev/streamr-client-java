package com.streamr.client.rest

import com.streamr.client.StreamrClient
import com.streamr.client.testing.TestingKeys
import com.streamr.client.testing.TestingStreamrClient
import com.streamr.client.testing.TestingStreams
import spock.lang.Specification

class PermissionEndpointsSpec extends Specification {

    private StreamrClient grantor
    private StreamrClient grantee

    void setup() {
        grantor = TestingStreamrClient.createClientWithPrivateKey(TestingKeys.generatePrivateKey())
        grantee = TestingStreamrClient.createClientWithPrivateKey(TestingKeys.generatePrivateKey())
    }

    void cleanup() {
        if (grantor != null) {
            grantor.disconnect()
        }
        if (grantee != null) {
            grantee.disconnect()
        }
    }

    void "grant()"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()
        Stream stream = grantor.createStream(proto)

        when:
        Permission p = grantor.grant(stream, Permission.Operation.stream_get, grantee.getPublisherId().toString())

        then:
        p.getId() != null
        p.getOperation() == Permission.Operation.stream_get
        p.getUser() == grantee.getPublisherId().toString()

        when:
        Stream granteeStream = grantee.getStream(stream.getId())

        then:
        granteeStream.getId() == stream.getId()
    }

    void "grantPublic()"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()
        Stream stream = grantor.createStream(proto)

        when:
        Permission p = grantor.grantPublic(stream, Permission.Operation.stream_get)

        then:
        p.getId() != null
        p.getOperation() == Permission.Operation.stream_get
        p.getAnonymous()

        when:
        Stream granteeStream = grantee.getStream(stream.getId())

        then:
        granteeStream.getId() == stream.getId()
    }
}
