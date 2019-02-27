package com.streamr.client;

import com.streamr.client.exceptions.InvalidOptionsException;

public class SigningOptions {
    private final String publishSigned; //"auto", "always" or "never"
    private final String verifySignatures; //"auto", "always" or "never"

    public SigningOptions(String publishSigned, String verifySignatures) {
        validateOption(publishSigned);
        validateOption(verifySignatures);
        this.publishSigned = publishSigned;
        this.verifySignatures = verifySignatures;
    }

    private void validateOption(String option) {
        if(!(option.equals("auto") || option.equals("always") || option.equals("never"))) {
            throw new InvalidOptionsException("Must be either 'auto', 'always' or 'never' but got: "+option);
        }
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
