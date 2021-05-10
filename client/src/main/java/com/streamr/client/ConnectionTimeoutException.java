package com.streamr.client;

public class ConnectionTimeoutException extends RuntimeException {
    public ConnectionTimeoutException(String url) {
        super("Connection timed out to URL: " + url);
    }
}
