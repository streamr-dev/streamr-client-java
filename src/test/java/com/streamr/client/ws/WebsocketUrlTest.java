package com.streamr.client.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.message_layer.StreamMessage;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class WebsocketUrlTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(WebsocketUrl.class).verify();
  }

  @Test
  void addsMissingQueryString() {
    String url = "some-url";
    String expectedUrl =
        url
            + "?controlLayerVersion="
            + ControlMessage.LATEST_VERSION
            + "&messageLayerVersion="
            + StreamMessage.LATEST_VERSION;
    WebsocketUrl wsUrl = new WebsocketUrl(url);
    assertEquals(expectedUrl, wsUrl.toString());
  }

  @Test
  void addsMissingControlLayerVersionParam() {
    String url = "some-url?messageLayerVersion=31";
    String expectedUrl = url + "&controlLayerVersion=" + ControlMessage.LATEST_VERSION;
    WebsocketUrl wsUrl = new WebsocketUrl(url);
    assertEquals(expectedUrl, wsUrl.toString());
  }

  @Test
  void addsMissingMessageLayerVersionParam() {
    String url = "some-url?controlLayerVersion=1";
    String expectedUrl = url + "&messageLayerVersion=" + StreamMessage.LATEST_VERSION;
    WebsocketUrl wsUrl = new WebsocketUrl(url);
    assertEquals(expectedUrl, wsUrl.toString());
  }

  @Test
  void addsMissingQueryStringInSetMethod() {
    String url = "some-url";
    String expectedUrl =
        url
            + "?controlLayerVersion="
            + ControlMessage.LATEST_VERSION
            + "&messageLayerVersion="
            + StreamMessage.LATEST_VERSION;
    WebsocketUrl wsUrl = new WebsocketUrl(url);
    assertEquals(expectedUrl, wsUrl.toString());
  }

  @Test
  void addsMissingControlLayerVersionParamInSetMethod() {
    String url = "some-url?messageLayerVersion=31";
    String expectedUrl = url + "&controlLayerVersion=" + ControlMessage.LATEST_VERSION;
    WebsocketUrl wsUrl = new WebsocketUrl(url);
    assertEquals(expectedUrl, wsUrl.toString());
  }

  @Test
  void addsMissingMessageLayerVersionParamInSetMethod() {
    String url = "some-url?controlLayerVersion=1";
    String expectedUrl = url + "&messageLayerVersion=" + StreamMessage.LATEST_VERSION;
    WebsocketUrl wsUrl = new WebsocketUrl(url);
    assertEquals(expectedUrl, wsUrl.toString());
  }

  @Test
  void byDefaultHasCorrectVersionsInWsUrl() {
    WebsocketUrl wsUrl = new WebsocketUrl();
    final String expectedUrl =
        "wss://www.streamr.com/api/v1/ws?controlLayerVersion="
            + ControlMessage.LATEST_VERSION
            + "&messageLayerVersion="
            + StreamMessage.LATEST_VERSION;
    assertEquals(expectedUrl, wsUrl.toString());
  }
}
