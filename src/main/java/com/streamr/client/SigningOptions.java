package com.streamr.client;

public class SigningOptions {
    private final String publishSigned; //"auto", "always" or "never"
    private final String verifySignatures; //"auto", "always" or "never"

    public SigningOptions(String publishSigned, String verifySignatures) {
        this.publishSigned = publishSigned;
        this.verifySignatures = verifySignatures;
    }

    public String getPublishSigned() {
        return publishSigned;
    }

    public String getVerifySignatures() {
        return verifySignatures;
    }

    static SigningOptions getDefault() {
        return new SigningOptions("auto", "auto");
    }
}
