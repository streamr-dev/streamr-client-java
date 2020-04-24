package com.streamr.client;

import com.streamr.client.protocol.control_layer.BroadcastMessage;
import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.control_layer.SubscribeResponse;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.rest.Stream;
import com.streamr.client.utils.KeyHistoryStorage;
import com.streamr.client.utils.MessageCreationUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

public class TestWebSocketServer extends WebSocketServer {
    private static final Logger log = LogManager.getLogger();
    private final MessageCreationUtil msgCreationUtil = new MessageCreationUtil("publisherId", null, new KeyHistoryStorage());
    private LinkedList<String> msgs = new LinkedList<>();
    private String wsUrl;

    public TestWebSocketServer(String host, int port) {
        super(new InetSocketAddress(host, port));
        wsUrl = "ws://" + this.getAddress().getHostString() + ":" + this.getAddress().getPort();
    }

    public void sendSubscribeToAll(String streamId, int partition) {
        log.info("sendSubscribe: connections list size is " + getConnections().size());
        SubscribeResponse subscribeResponse = new SubscribeResponse(streamId, partition);
        getConnections().forEach(webSocket -> {
            log.info("sendSubscribe: " + subscribeResponse.toJson());
            webSocket.send(subscribeResponse.toJson());
        });
    }

    public void sendToAll(Stream stream, Map<String, Object> payload) {
        log.info("sendToAll: connections list size is " + getConnections().size());
        StreamMessage streamMessage = msgCreationUtil.createStreamMessage(stream, payload, new Date(), null);
        BroadcastMessage req = new BroadcastMessage(streamMessage);
        getConnections().forEach((webSocket -> {
            log.info("send: " + req.toJson());
            webSocket.send(req.toJson());
        }));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.info("onOpen");
        conn.sendPing();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.info("onClose");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        log.info("onMessage: " + message);
        msgs.add(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.info("onError: " + ex);
    }

    @Override
    public void onStart() {
        log.info("onStart");
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        log.info("stop");
        super.stop();
    }

    public void clear() {
        msgs = new LinkedList<>();
    }

    public boolean expect(ControlMessage msg) {
        if (msgs.isEmpty()) {
            try {
                Thread.sleep(50); // give time to the client to send the expected request
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String received = msgs.poll();
        String expected = msg.toJson();
        boolean res = received != null && received.equals(expected);
        if (!res) {
            System.out.println("Expected: "+expected);
            System.out.println("But received: "+received);
        }
        return res;
    }

    public boolean noOtherMessagesReceived() {
        boolean res = msgs.isEmpty();
        if (!res) {
            System.out.println("Unexpected received messages:");
            for (String msg: msgs) {
                System.out.println(msg);
            }
        }
        return res;
    }

    public String getWsUrl() {
        return wsUrl;
    }
}
