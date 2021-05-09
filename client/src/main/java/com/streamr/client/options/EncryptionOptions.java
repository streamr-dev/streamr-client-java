package com.streamr.client.options;

import com.streamr.client.crypto.Keys;
import com.streamr.client.utils.EncryptionUtil;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class EncryptionOptions {
  private RSAPublicKey rsaPublicKey;
  private RSAPrivateKey rsaPrivateKey;
  private final boolean autoRevoke;

  public EncryptionOptions(
      String rsaPublicKey, String rsaPrivateKey, boolean autoRevoke) {
    if (rsaPublicKey != null) {
      Keys.validatePublicKey(rsaPublicKey);
      this.rsaPublicKey = EncryptionUtil.getPublicKeyFromString(rsaPublicKey);
    }
    if (rsaPrivateKey != null) {
      Keys.validatePrivateKey(rsaPrivateKey);
      this.rsaPrivateKey = EncryptionUtil.getPrivateKeyFromString(rsaPrivateKey);
    }
    this.autoRevoke = autoRevoke;
  }

  public RSAPublicKey getRsaPublicKey() {
    return rsaPublicKey;
  }

  public RSAPrivateKey getRsaPrivateKey() {
    return rsaPrivateKey;
  }

  public boolean autoRevoke() {
    return autoRevoke;
  }

  public static EncryptionOptions getDefault() {
    return new EncryptionOptions(null, null, true);
  }
}
