package com.streamr.client.options

import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.message_layer.StreamMessage
import spock.lang.Specification

class StreamrClientOptionsSpec extends Specification {
    void "adds missing query string"() {
        String url = "some-url"
        String expectedUrl = url + "?controlLayerVersion=" + ControlMessage.LATEST_VERSION + "&messageLayerVersion=" + StreamMessage.LATEST_VERSION
        when:
        StreamrClientOptions options = new StreamrClientOptions(SigningOptions.getDefault(), EncryptionOptions.getDefault(), url)
        then:
        options.getWebsocketApiUrl() == expectedUrl
    }
    void "adds missing control layer version param"() {
        String url = "some-url?messageLayerVersion=31"
        String expectedUrl = url + "&controlLayerVersion=" + ControlMessage.LATEST_VERSION
        when:
        StreamrClientOptions options = new StreamrClientOptions(SigningOptions.getDefault(), EncryptionOptions.getDefault(), url)
        then:
        options.getWebsocketApiUrl() == expectedUrl
    }
    void "adds missing message layer version param"() {
        String url = "some-url?controlLayerVersion=1"
        String expectedUrl = url + "&messageLayerVersion=" + StreamMessage.LATEST_VERSION
        when:
        StreamrClientOptions options = new StreamrClientOptions(SigningOptions.getDefault(), EncryptionOptions.getDefault(), url)
        then:
        options.getWebsocketApiUrl() == expectedUrl
    }
    void "by default has correct versions in ws url"() {
        when:
        StreamrClientOptions options = new StreamrClientOptions()
        then:
        options.getWebsocketApiUrl() == "wss://www.streamr.com/api/v1/ws?controlLayerVersion=${ControlMessage.LATEST_VERSION}&messageLayerVersion=${StreamMessage.LATEST_VERSION}"
    }
}
