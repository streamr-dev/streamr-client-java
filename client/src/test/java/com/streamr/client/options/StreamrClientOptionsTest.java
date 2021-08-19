package com.streamr.client.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.options.SigningOptions;
import com.streamr.client.protocol.utils.EncryptionUtil;
import org.junit.jupiter.api.Test;

class StreamrClientOptionsTest {
  @Test
  void addsMissingQueryString() {
    String url = "some-url";
    String expectedUrl =
        url
            + "?controlLayerVersion="
            + ControlMessage.LATEST_VERSION
            + "&messageLayerVersion="
            + StreamMessage.LATEST_VERSION;

    StreamrClientOptions options =
        new StreamrClientOptions(SigningOptions.getDefault(), EncryptionOptions.getDefault(), url);
    assertEquals(expectedUrl, options.getWebsocketApiUrl());
  }

  @Test
  void addsMissingControlLayerVersionParam() {
    String url = "some-url?messageLayerVersion=31";
    String expectedUrl = url + "&controlLayerVersion=" + ControlMessage.LATEST_VERSION;
    StreamrClientOptions options =
        new StreamrClientOptions(SigningOptions.getDefault(), EncryptionOptions.getDefault(), url);
    assertEquals(expectedUrl, options.getWebsocketApiUrl());
  }

  @Test
  void addsMissingMessageLayerVersionParam() {
    String url = "some-url?controlLayerVersion=1";
    String expectedUrl = url + "&messageLayerVersion=" + StreamMessage.LATEST_VERSION;
    StreamrClientOptions options =
        new StreamrClientOptions(SigningOptions.getDefault(), EncryptionOptions.getDefault(), url);
    assertEquals(expectedUrl, options.getWebsocketApiUrl());
  }

  @Test
  void throwsIfInvalidPublicKeyPassedToConstructor() {
    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () -> {
              new EncryptionOptions(null, "wrong-format", null, true);
            });
    assertEquals("Must be a valid RSA public key in the PEM format.", e.getMessage());
  }

  @Test
  void throwsIfInvalidPrivateKeyPassedToConstructor() {
    EncryptionUtil util = new EncryptionUtil();
    String publicKeyAsPemString = util.getPublicKeyAsPemString();
    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () -> {
              new EncryptionOptions(null, publicKeyAsPemString, "wrong-format", true);
            });
    assertEquals("Must be a valid RSA private key in the PEM format.", e.getMessage());
    ;
  }

  @Test
  void byDefaultHasCorrectVersionsInWsUrl() {
    StreamrClientOptions options = new StreamrClientOptions();
    String expected =
        String.format(
            "wss://www.streamr.com/api/v1/ws?controlLayerVersion=%s&messageLayerVersion=%s",
            ControlMessage.LATEST_VERSION, StreamMessage.LATEST_VERSION);
    assertEquals(expected, options.getWebsocketApiUrl());
  }
}
