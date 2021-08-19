package com.streamr.client.utils;

public class SignatureVerificationResult {
  private boolean correct;
  private Boolean validPublisher;

  private SignatureVerificationResult(boolean correct, Boolean validPublisher) {
    this.correct = correct;
    this.validPublisher = validPublisher;
  }

  public static SignatureVerificationResult fromBoolean(boolean correct) {
    return new SignatureVerificationResult(correct, null);
  }

  public static SignatureVerificationResult withValidPublisher(boolean isValidSignature) {
    return new SignatureVerificationResult(isValidSignature, true);
  }

  public static SignatureVerificationResult invalidPublisher() {
    return new SignatureVerificationResult(false, false);
  }

  public boolean isCorrect() {
    return correct;
  }

  public Boolean isValidPublisher() {
    return validPublisher;
  }
}
