package com.streamr.client.utils;

import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.subs.Subscription;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;

public class OneTimeResend extends Thread {
    private static final Logger log = LogManager.getLogger();

    private WebSocketClient ws;
    private ControlMessage controlMessage;
    private int timeout;
    private Subscription sub;

    public OneTimeResend(WebSocketClient ws, ControlMessage controlMessage, int timeout, Subscription sub) {
        this.ws = ws;
        this.controlMessage = controlMessage;
        this.timeout = timeout;
        this.sub = sub;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(timeout);
            if (!isInterrupted() && ws.isOpen() && sub.isSubscribed()) {
                ws.send(controlMessage.toJson());
            }
        } catch (InterruptedException e) {
            log.error(e);
        }
    }
}
