package com.streamr.client.exceptions;

public class UnableToDecryptException extends RuntimeException {

    public UnableToDecryptException(String ciphertext) {
        super("Unable to decrypt: " + (ciphertext.length() > 100 ? ciphertext.substring(0, 100) + "..." : ciphertext));
    }

}
