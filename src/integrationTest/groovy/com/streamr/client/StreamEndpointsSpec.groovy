package com.streamr.client

import com.streamr.client.exceptions.AuthenticationException
import com.streamr.client.exceptions.PermissionDeniedException
import com.streamr.client.exceptions.ResourceNotFoundException
import com.streamr.client.rest.FieldConfig
import com.streamr.client.rest.Stream
import com.streamr.client.rest.StreamConfig

class StreamEndpointsSpec extends StreamrIntegrationSpecification {

	private StreamrClient client

	void setup() {
		client = createClient("tester1-api-key");
	}

    void cleanup() {
        if (client != null && client.state != StreamrWebsocketClient.State.Disconnected) {
            client.disconnect()
        }
    }

	/*
	 * Stream endpoint tests
	 */

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

    void "createStream() throws AuthenticationRequiredException if the client is unauthenticated"() {
        Stream proto = new Stream(generateResourceName(), "This stream was created from an integration test")
        StreamrClient unauthenticatedClient = createClient(null);

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
        StreamrClient unauthenticatedClient = createClient(null);

        when:
        Stream createResult = client.createStream(proto)

        then:
        createResult.id != null

        when:
        unauthenticatedClient.getStream(createResult.id)

        then:
        thrown(PermissionDeniedException)
    }
}
