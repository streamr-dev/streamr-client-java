package com.streamr.client.utils;

import com.streamr.client.Subscription;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.protocol.control_layer.ResendRangeRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;

public class PeriodicResend extends Thread {
    private static final Logger log = LogManager.getLogger();

    private WebSocketClient ws;
    private int interval;
    private Subscription sub;
    private String publisherId;
    private String msgChainId;
    private String sessionToken;
    public PeriodicResend(WebSocketClient ws, int interval, Subscription sub,
                          String publisherId, String msgChainId, String sessionToken) {
        this.ws = ws;
        this.interval = interval;
        this.sub = sub;
        this.publisherId = publisherId;
        this.msgChainId = msgChainId;
        this.sessionToken = sessionToken;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(interval); //initial delay before the first resend request
        } catch (InterruptedException e) {
            log.error(e);
        }
        GapDetectedException ex;
        while(ws.isOpen() && sub.isSubscribed() && (ex = sub.getGapDetectedException(publisherId, msgChainId)) != null) {
            ResendRangeRequest req = new ResendRangeRequest(ex.getStreamId(), ex.getStreamPartition(),
                    sub.getId(), ex.getFrom(), ex.getTo(), publisherId, msgChainId, sessionToken);
            ws.send(req.toJson());
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                log.error(e);
            }
        }
    }
}
