package com.streamr.client.exceptions;

public class InvalidGroupKeyException extends Exception {

    public InvalidGroupKeyException(int keyLength) {
        super("Group key must be 256 bits long, but got a key length of " + keyLength + " bits.");
    }

}
