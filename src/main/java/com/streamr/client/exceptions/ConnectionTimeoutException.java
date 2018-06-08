package com.streamr.client.exceptions;

public class ConnectionTimeoutException extends Exception {
    public ConnectionTimeoutException(String url) {
        super("Connection timed out to URL: " + url);
    }
}
