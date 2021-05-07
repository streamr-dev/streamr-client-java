package com.streamr.client.rest

import com.streamr.client.StreamrClient
import com.streamr.client.crypto.Keys
import com.streamr.client.testing.TestingAddresses
import com.streamr.client.testing.TestingKeys
import com.streamr.client.testing.TestingStreamrClient
import com.streamr.client.testing.TestingStreams
import spock.lang.Specification

class StreamEndpointsSpec extends Specification {
    private final BigInteger privateKey = TestingKeys.generatePrivateKey()
    private StreamrClient client

    void setup() {
        client = TestingStreamrClient.createClientWithPrivateKey(privateKey)
    }

    void cleanup() {
        if (client != null) {
            client.disconnect()
        }
    }

    void "createStream() then getStream()"() {
        def fieldFoo = new FieldConfig("foo", FieldConfig.Type.NUMBER)
        def fieldBar = new FieldConfig("bar", FieldConfig.Type.STRING)
        def config = new StreamConfig(fieldFoo, fieldBar)
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .withConfig(config)
                .createStream()

        when:
        Stream createResult = client.createStream(proto)

        then:
        createResult.id != null
        createResult.name == proto.name
        createResult.description == proto.description
        createResult.config == proto.config
        createResult.partitions == 1
        !createResult.uiChannel

        when:
        Stream getResult = client.getStream(createResult.getId())

        then:
        getResult.id == createResult.id
        getResult.name == createResult.name
        getResult.description == createResult.description
        getResult.config == createResult.config
        getResult.partitions == createResult.partitions
        getResult.uiChannel == createResult.uiChannel
        !getResult.requiresSignedData()
    }

    void "createStream() then getStream() setting requireSignedData"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .withRequireSignedData(true)
                .withConfig(new StreamConfig())
                .createStream()

        when:
        Stream createResult = client.createStream(proto)

        then:
        createResult.requiresSignedData()

        when:
        Stream getResult = client.getStream(createResult.getId())

        then:
        getResult.requiresSignedData()
    }

    void "createStream() then getStreamByName()"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()

        when:
        Stream createResult = client.createStream(proto)

        then:
        createResult.id != null
        createResult.name == proto.name

        when:
        Stream getResult = client.getStreamByName(proto.name)

        then:
        getResult.id == createResult.id
        getResult.name == createResult.name
    }


    void "getStreamByName() throws ResourceNotFoundException if no such stream is found"() {
        when:
        client.getStreamByName("non-existent for sure " + System.currentTimeMillis())

        then:
        thrown(ResourceNotFoundException)
    }

    void "getStreamByName() throws AmbiguousResultsException if multiple matching streams are found"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()

        // Create 2 streams with same name
        client.createStream(proto)
        client.createStream(proto)

        when:
        client.getStreamByName(proto.getName())

        then:
        thrown(AmbiguousResultsException)
    }

    void "createStream() throws AuthenticationException if the client is unauthenticated"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()
        StreamrClient unauthenticatedClient = TestingStreamrClient.createUnauthenticatedClient()

        when:
        unauthenticatedClient.createStream(proto)

        then:
        thrown(AuthenticationException)

        cleanup:
        unauthenticatedClient.disconnect()
    }

    void "getStream() throws StreamNotFoundException for non-existent streams"() {
        when:
        client.getStream("non-existent")

        then:
        thrown(ResourceNotFoundException)
    }

    void "getStream() throws PermissionDeniedException for streams which the user can not access"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()
        StreamrClient unauthenticatedClient = TestingStreamrClient.createUnauthenticatedClient()

        when:
        Stream createResult = client.createStream(proto)

        then:
        createResult.id != null

        when:
        unauthenticatedClient.getStream(createResult.id)

        then:
        thrown(PermissionDeniedException)

        cleanup:
        unauthenticatedClient.disconnect()
    }

    void "getUserInfo()"() {
        when:
        UserInfo info = client.getUserInfo()

        then:
        info.getName() == "Anonymous User"
        info.getUsername() == Keys.privateKeyToAddressWithPrefix(privateKey)
    }

    void "getPublishers()"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()
        Stream createdResult = client.createStream(proto)
        when:
        List<String> publishers = client.getPublishers(createdResult.id)
        then:
        publishers == [client.getPublisherId().toString()]
    }

    void "isPublisher()"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()
        Stream createdResult = client.createStream(proto)
        when:
        boolean isValid1 = client.isPublisher(createdResult.id, client.getPublisherId())
        boolean isValid2 = client.isPublisher(createdResult.id, "wrong-address")
        then:
        isValid1
        !isValid2
    }

    void "getSubscribers()"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()
        Stream createdResult = client.createStream(proto)
        when:
        List<String> subscribers = client.getSubscribers(createdResult.id)
        then:
        subscribers == [client.getPublisherId().toString()]
    }

    void "isSubscriber()"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()
        Stream createdResult = client.createStream(proto)
        when:
        boolean isValid1 = client.isSubscriber(createdResult.id, client.getPublisherId().toString())
        boolean isValid2 = client.isSubscriber(createdResult.id, "wrong-address")
        then:
        isValid1
        !isValid2
    }

    void "addStreamToStorageNode"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()
        String streamId = client.createStream(proto).getId()
        StorageNode storageNode = new StorageNode(TestingAddresses.createRandom())
        when:
        client.addStreamToStorageNode(streamId, storageNode)
        List<StorageNode> storageNodes = client.getStorageNodes(streamId)
        then:
        storageNodes.size() == 1
        storageNodes.get(0).getAddress() == storageNode.getAddress()
    }

    void "removeStreamFromStorageNode"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .createStream()
        String streamId = client.createStream(proto).getId()
        StorageNode storageNode = new StorageNode(TestingAddresses.createRandom())
        client.addStreamToStorageNode(streamId, storageNode)
        when:
        client.removeStreamToStorageNode(streamId, storageNode)
        List<StorageNode> storageNodes = client.getStorageNodes(streamId)
        then:
        storageNodes.size() == 0
    }

    void "getStreamPartsByStorageNode"() {
        Stream proto = new Stream.Builder()
                .withName(TestingStreams.generateName())
                .withDescription("This stream was created from an integration test")
                .withPartitions(2)
                .createStream()
        String streamId = client.createStream(proto).getId()
        StorageNode storageNode = new StorageNode(TestingAddresses.createRandom())
        client.addStreamToStorageNode(streamId, storageNode)
        when:
        List<StreamPart> streamParts = client.getStreamPartsByStorageNode(storageNode)
        then:
        streamParts.size() == 2
        streamParts.get(0).getStreamId() == streamId
        streamParts.get(0).getStreamPartition() == 0
        streamParts.get(1).getStreamId() == streamId
        streamParts.get(1).getStreamPartition() == 1
    }

    void "not same token used after logout()"() {
        when:
        client.getUserInfo() // fetches sessionToken1 and requests endpoint
        String sessionToken1 = client.getSessionToken()
        client.logout()
        client.getUserInfo() // requests with sessionToken1, receives 401, fetches sessionToken2 and requests endpoint
        String sessionToken2 = client.getSessionToken()
        then:
        sessionToken1 != sessionToken2
    }

    void "throws if logout() when already logged out"() {
        when:
        client.logout()
        client.logout() // does not retry with a new session token after receiving 401
        then:
        thrown(AuthenticationException)
    }
}
