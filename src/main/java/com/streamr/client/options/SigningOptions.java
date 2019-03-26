package com.streamr.client.options;

public class SigningOptions {
    private final SignatureComputationPolicy publishSigned;
    private final SignatureVerificationPolicy verifySignatures;

    public enum SignatureComputationPolicy {
        AUTO,
        ALWAYS,
        NEVER
    }

    public enum SignatureVerificationPolicy {
        AUTO,
        ALWAYS,
        NEVER,
    }

    public SigningOptions(SignatureComputationPolicy publishSigned, SignatureVerificationPolicy verifySignatures) {
        this.publishSigned = publishSigned;
        this.verifySignatures = verifySignatures;
    }

    public SignatureComputationPolicy getPublishSigned() {
        return publishSigned;
    }

    public SignatureVerificationPolicy getVerifySignatures() {
        return verifySignatures;
    }

    public static SigningOptions getDefault() {
        return new SigningOptions(SignatureComputationPolicy.AUTO, SignatureVerificationPolicy.AUTO);
    }
}
