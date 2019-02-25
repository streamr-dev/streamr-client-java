package com.streamr.client

import com.streamr.client.authentication.ChallengeAuthenticationMethod
import com.streamr.client.exceptions.AmbiguousResultsException
import com.streamr.client.exceptions.PermissionDeniedException
import com.streamr.client.exceptions.ResourceNotFoundException
import com.streamr.client.rest.Stream
import com.streamr.client.rest.StreamConfig
import com.streamr.client.exceptions.AuthenticationException
import com.streamr.client.rest.FieldConfig
import com.streamr.client.rest.UserInfo

class StreamEndpointsSpec extends StreamrIntegrationSpecification {

	private StreamrClient client

	void setup() {
		client = createClientWithPrivateKey(generatePrivateKey())
	}

    void cleanup() {
        if (client != null && client.state != StreamrClient.State.Disconnected) {
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
        StreamrRESTClient unauthenticatedClient = createUnauthenticatedClient()

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
        StreamrRESTClient unauthenticatedClient = createUnauthenticatedClient()

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
        ChallengeAuthenticationMethod method = (ChallengeAuthenticationMethod) client.getOptions().getAuthenticationMethod()
        when:
        UserInfo info = client.getUserInfo()

        then:
        info.getName() == method.address
        info.getUsername() == method.address
    }

    void "getUserInfo() with api key"() {
        StreamrClient apiKeyClient = createClientWithApiKey("tester1-api-key")
        when:
        UserInfo info = apiKeyClient.getUserInfo()

        then:
        info.getName() == "Tester One"
        info.getUsername() == "tester1@streamr.com"
    }
}
