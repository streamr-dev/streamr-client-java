package com.streamr.client.subs;

import com.streamr.client.protocol.control_layer.ControlMessage;
import org.java_websocket.client.WebSocketClient;

public class OneTimeResend extends Thread {
  private WebSocketClient ws;
  private ControlMessage controlMessage;
  private int timeout;
  private Subscription sub;

  public OneTimeResend(
      WebSocketClient ws, ControlMessage controlMessage, int timeout, Subscription sub) {
    this.ws = ws;
    this.controlMessage = controlMessage;
    this.timeout = timeout;
    this.sub = sub;
  }

  @Override
  public void run() {
    try {
      Thread.sleep(timeout);
      if (ws.isOpen() && sub.isSubscribed()) {
        ws.send(controlMessage.toJson());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
