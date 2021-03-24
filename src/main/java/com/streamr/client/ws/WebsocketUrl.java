package com.streamr.client.ws;

import com.streamr.client.java.util.Objects;
import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.message_layer.StreamMessage;

public final class WebsocketUrl {
  private static final String URL_PARAMTERS =
      "?controlLayerVersion="
          + ControlMessage.LATEST_VERSION
          + "&messageLayerVersion="
          + StreamMessage.LATEST_VERSION;
  private static final String DEFAULT = "wss://www.streamr.com/api/v1/ws" + URL_PARAMTERS;
  private final String url;

  public WebsocketUrl() {
    this.url = DEFAULT;
  }

  public WebsocketUrl(final String url) {
    Objects.requireNonNull(url);
    this.url = addMissingQueryString(url);
  }

  private String addMissingQueryString(final String url) {
    String[] parts = url.split("\\?");
    if (parts.length == 1) { // no query string
      return url + URL_PARAMTERS;
    } else {
      String[] params = parts[1].split("&");
      boolean missingControlLayer = true;
      boolean missingMessageLayer = true;
      for (String p : params) {
        if (p.startsWith("controlLayerVersion=")) {
          missingControlLayer = false;
        } else if (p.startsWith("messageLayerVersion=")) {
          missingMessageLayer = false;
        }
      }
      String result = url;
      if (missingControlLayer) {
        result += "&controlLayerVersion=" + ControlMessage.LATEST_VERSION;
      }
      if (missingMessageLayer) {
        result += "&messageLayerVersion=" + StreamMessage.LATEST_VERSION;
      }
      return result;
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final WebsocketUrl that = (WebsocketUrl) obj;
    return Objects.equals(url, that.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url);
  }

  @Override
  public String toString() {
    return url;
  }
}
