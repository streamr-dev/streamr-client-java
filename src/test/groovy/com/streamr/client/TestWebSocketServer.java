package com.streamr.client;

import com.streamr.client.protocol.control_layer.BroadcastMessage;
import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.control_layer.SubscribeRequest;
import com.streamr.client.protocol.control_layer.SubscribeResponse;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.rest.Stream;
import com.streamr.client.utils.Address;
import com.streamr.client.utils.MessageCreationUtil;
import junit.framework.AssertionFailedError;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class TestWebSocketServer extends WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(TestWebSocketServer.class);
    private final MessageCreationUtil msgCreationUtil = new MessageCreationUtil(Address.createRandom(), null);
    private final LinkedList<ReceivedControlMessage> receivedControlMessages = new LinkedList<>();
    private final String wsUrl;
    private int checkedControlMessages = 0;

    public TestWebSocketServer(String host, int port) {
        super(new InetSocketAddress(host, port));
        wsUrl = "ws://" + this.getAddress().getHostString() + ":" + this.getAddress().getPort();
    }

    public void sendTo(WebSocket conn, ControlMessage message) {
        if (!getConnections().contains(conn)) {
            throw new RuntimeException("Connection does not exist: "+conn);
        }
        conn.send(message.toJson());
    }

    public void broadcastMessageToAll(Stream stream, Map<String, Object> payload) {
        if (getConnections().isEmpty()) {
            throw new IllegalStateException("Tried to broadcast a message, but there are no connected clients! Something's wrong!");
        }
        log.info("sendToAll: connections list size is " + getConnections().size());
        StreamMessage streamMessage = msgCreationUtil.createStreamMessage(stream, payload, new Date());
        BroadcastMessage req = new BroadcastMessage("", streamMessage);
        getConnections().forEach((webSocket -> {
            log.info("send: " + req.toJson());
            webSocket.send(req.toJson());
        }));
    }

    public void respondTo(ReceivedControlMessage receivedControlMessage) {
        if (receivedControlMessage.message instanceof SubscribeRequest) {
            SubscribeRequest request = (SubscribeRequest) receivedControlMessage.getMessage();
            receivedControlMessage.getConnection().send(
                    new SubscribeResponse(request.getRequestId(), request.getStreamId(), request.getStreamPartition()).toJson()
            );
        } else {
            throw new RuntimeException("Haven't implemented a default response to a " + receivedControlMessage.getMessage());
        }
    }

    public List<ReceivedControlMessage> getReceivedControlMessages() {
        return Collections.unmodifiableList(receivedControlMessages);
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
        try {
            receivedControlMessages.add(new ReceivedControlMessage(
                    ControlMessage.fromJson(message),
                    conn
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        receivedControlMessages.clear();
        checkedControlMessages = 0;
    }

    public void expect(ControlMessage expected) {
        if (receivedControlMessages.size() == checkedControlMessages) {
            try {
                Thread.sleep(100); // give time to the client to send the expected request
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (receivedControlMessages.size() == checkedControlMessages) {
            throw new AssertionFailedError("Expected message was not received at all: " + expected.toJson());
        }

        // Get first unchecked message
        ControlMessage received = receivedControlMessages.get(checkedControlMessages).getMessage();

        if (received == null || !received.equals(expected)) {
            throw new AssertionFailedError("\nExpected: "+expected.toJson()+"\nReceived: "+received.toJson());
        } else {
            checkedControlMessages++;
        }
    }

    public void noOtherMessagesReceived() {
        if (receivedControlMessages.size() > checkedControlMessages) {
            StringBuilder sb = new StringBuilder("Unexpected received messages:");

            for (ReceivedControlMessage msg: receivedControlMessages) {
                sb.append("\n");
                sb.append(msg.getMessage().toJson());
            }

            throw new AssertionFailedError(sb.toString());
        }
    }



    public String getWsUrl() {
        return wsUrl;
    }

    public static class ReceivedControlMessage {
        private final ControlMessage message;
        private final WebSocket connection;

        public ReceivedControlMessage(ControlMessage message, WebSocket connection) {
            this.message = message;
            this.connection = connection;
        }

        public ControlMessage getMessage() {
            return message;
        }

        public WebSocket getConnection() {
            return connection;
        }

        @Override
        public String toString() {
            return message.getClass().getSimpleName() + ": " + message.toJson();
        }
    }
}
