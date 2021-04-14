package com.streamr.client

import com.streamr.client.authentication.EthereumAuthenticationMethod
import com.streamr.client.exceptions.AmbiguousResultsException
import com.streamr.client.exceptions.PermissionDeniedException
import com.streamr.client.exceptions.ResourceNotFoundException
import com.streamr.client.rest.Stream
import com.streamr.client.rest.StreamConfig
import com.streamr.client.exceptions.AuthenticationException
import com.streamr.client.rest.FieldConfig
import com.streamr.client.rest.UserInfo
import com.streamr.client.rest.StorageNode
import com.streamr.client.rest.StreamPart
import com.streamr.client.utils.Address

import java.util.Arrays

class StreamEndpointsSpec extends StreamrIntegrationSpecification {

	private StreamrClient client

	void setup() {
		client = createClientWithPrivateKey(generatePrivateKey())
	}

    void cleanup() {
        if (client != null) {
            client.disconnect()
        }
    }

    void "createStream() then getStream()"() {
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        proto.setConfig(new StreamConfig()
                            .addField(new FieldConfig("foo", FieldConfig.Type.NUMBER))
                            .addField(new FieldConfig("bar", FieldConfig.Type.STRING)))

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
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        proto.requireSignedData = true
        proto.setConfig(new StreamConfig())

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
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")

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
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")

        // Create 2 streams with same name
        client.createStream(proto)
        client.createStream(proto)

        when:
        client.getStreamByName(proto.getName())

        then:
        thrown(AmbiguousResultsException)
    }

    void "createStream() throws AuthenticationException if the client is unauthenticated"() {
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        StreamrClient unauthenticatedClient = createUnauthenticatedClient()

        when:
        unauthenticatedClient.createStream(proto)

        then:
        thrown(AuthenticationException)
    }

    void "getStream() throws StreamNotFoundException for non-existent streams"() {
        when:
        client.getStream("non-existent")

        then:
        thrown(ResourceNotFoundException)
    }

    void "getStream() throws PermissionDeniedException for streams which the user can not access"() {
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        StreamrClient unauthenticatedClient = createUnauthenticatedClient()

        when:
        Stream createResult = client.createStream(proto)

        then:
        createResult.id != null

        when:
        unauthenticatedClient.getStream(createResult.id)

        then:
        thrown(PermissionDeniedException)
    }

    void "getUserInfo()"() {
        EthereumAuthenticationMethod method = (EthereumAuthenticationMethod) client.getOptions().getAuthenticationMethod()
        when:
        UserInfo info = client.getUserInfo()

        then:
        info.getName() == "Anonymous User"
        info.getUsername() == method.address
    }

    void "getPublishers()"() {
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        Stream createdResult = client.createStream(proto)
        when:
        List<Address> publishers = client.getPublishers(createdResult.id).collect { p -> new Address(p) }
        then:
        publishers == [client.getPublisherId()]
    }

    void "isPublisher()"() {
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        Stream createdResult = client.createStream(proto)
        when:
        boolean isValid1 = client.isPublisher(createdResult.id, client.getPublisherId())
        boolean isValid2 = client.isPublisher(createdResult.id, "wrong-address")
        then:
        isValid1
        !isValid2
    }

    void "getSubscribers()"() {
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        Stream createdResult = client.createStream(proto)
        when:
        List<String> subscribers = client.getSubscribers(createdResult.id).collect { s -> new Address(s) }
        then:
        subscribers == [client.getPublisherId()]
    }

    void "isSubscriber()"() {
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        Stream createdResult = client.createStream(proto)
        when:
        boolean isValid1 = client.isSubscriber(createdResult.id, client.getPublisherId().toString())
        boolean isValid2 = client.isSubscriber(createdResult.id, "wrong-address")
        then:
        isValid1
        !isValid2
    }

    void "StorageNode.addStream"() {
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        String streamId = client.createStream(proto).getId()
        StorageNode devStorageNode = getDevStorageNode()
        when:
        client.addStreamToStorageNode(streamId, devStorageNode)
        List<StorageNode> storageNodes = client.getStorageNodes(streamId)
        then:
        storageNodes.size() == 1
        storageNodes.get(0).getAddress() == devStorageNode.getAddress()
    }

    void "StorageNode.removeStream"() {
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        String streamId = client.createStream(proto).getId()
        StorageNode devStorageNode = getDevStorageNode()
        client.addStreamToStorageNode(streamId, devStorageNode)
        when:
        client.removeStreamFromStorageNode(streamId, devStorageNode)
        List<StorageNode> storageNodes = client.getStorageNodes(streamId)
        then:
        storageNodes.size() == 0
    }

    void "StorageNode.getStreamParts"() {
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        proto.setPartitions(2)
        String streamId = client.createStream(proto).getId()
        StorageNode devStorageNode = getDevStorageNode()
        client.addStreamToStorageNode(streamId, devStorageNode)
        List<StreamPart> expectedStreamParts = Arrays.asList(new StreamPart(streamId, 0), new StreamPart(streamId, 1))
        when:
        List<StreamPart> actualStreamParts = client.getStreamPartsByStorageNode(devStorageNode)
        then:
        actualStreamParts.containsAll(expectedStreamParts)
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
