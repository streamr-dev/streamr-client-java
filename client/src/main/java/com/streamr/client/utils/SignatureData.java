package com.streamr.client.utils;

import java.util.Arrays;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public final class SignatureData extends Sign.SignatureData {
  public SignatureData(Sign.SignatureData signature) {
    super(signature.getV(), signature.getR(), signature.getS());
  }

  public byte[] toBytes65() {
    byte[] result = new byte[65];
    System.arraycopy(this.getR(), 0, result, 0, 32);
    System.arraycopy(this.getS(), 0, result, 32, 32);
    System.arraycopy(this.getV(), 0, result, 64, 1);
    return result;
  }

  public static SignatureData fromBytes65(byte[] bytes) {
    byte[] r = Arrays.copyOfRange(bytes, 0, 32);
    byte[] s = Arrays.copyOfRange(bytes, 32, 64);
    byte[] v = Arrays.copyOfRange(bytes, 64, 65);
    Sign.SignatureData data = new Sign.SignatureData(v, r, s);
    SignatureData signatureData = new SignatureData(data);
    return signatureData;
  }

  public String toHex() {
    byte[] bytes = this.toBytes65();
    return Numeric.toHexString(bytes);
  }
}
