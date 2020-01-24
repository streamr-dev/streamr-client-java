package com.streamr.client.exceptions;

public class InvalidRSAKeyException extends RuntimeException {

    public InvalidRSAKeyException(boolean isPublic) {
        super("Must be a valid RSA " + (isPublic ? "public" : "private") + " key in the PEM format.");
    }

}
