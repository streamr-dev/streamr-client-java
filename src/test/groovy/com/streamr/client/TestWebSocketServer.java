package com.streamr.client;

import com.streamr.client.protocol.control_layer.ControlMessage;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.function.Function;

public class TestWebSocketServer extends WebSocketServer {
    private LinkedList<String> msgs = new LinkedList<>();
    private String wsUrl;

    public TestWebSocketServer(String host, int port) {
        super(new InetSocketAddress(host, port));
        wsUrl = "ws://" + this.getAddress().getHostString() + ":" + this.getAddress().getPort();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.sendPing();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        msgs.add(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    @Override
    public void onStart() {

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
