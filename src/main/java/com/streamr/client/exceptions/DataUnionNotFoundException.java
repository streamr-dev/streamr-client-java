package com.streamr.client.exceptions;

public class DataUnionNotFoundException extends RuntimeException {
    public DataUnionNotFoundException(String mainnetAddress) {
        super("Data Union not found at address " + mainnetAddress);
    }
}
