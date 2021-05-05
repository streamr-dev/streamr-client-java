package com.streamr.client.options;

import com.streamr.client.utils.EncryptionUtil;
import com.streamr.client.utils.GroupKeyStore;
import com.streamr.client.utils.InMemoryGroupKeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class EncryptionOptions {
  private final GroupKeyStore keyStore;
  private RSAPublicKey rsaPublicKey;
  private RSAPrivateKey rsaPrivateKey;
  private final boolean autoRevoke;

  public EncryptionOptions(
      GroupKeyStore keyStore, String rsaPublicKey, String rsaPrivateKey, boolean autoRevoke) {
    this.keyStore = keyStore;
    if (rsaPublicKey != null) {
      EncryptionUtil.validatePublicKey(rsaPublicKey);
      this.rsaPublicKey = EncryptionUtil.getPublicKeyFromString(rsaPublicKey);
    }
    if (rsaPrivateKey != null) {
      EncryptionUtil.validatePrivateKey(rsaPrivateKey);
      this.rsaPrivateKey = EncryptionUtil.getPrivateKeyFromString(rsaPrivateKey);
    }
    this.autoRevoke = autoRevoke;
  }

  private EncryptionOptions(GroupKeyStore keyStore, String rsaPublicKey, String rsaPrivateKey) {
    this(keyStore, rsaPublicKey, rsaPrivateKey, true);
  }

  // TODO: non-null RSA defaults?
  private EncryptionOptions(GroupKeyStore keyStore) {
    this(keyStore, null, null, true);
  }

  private EncryptionOptions() {
    this(new InMemoryGroupKeyStore(), null, null);
  }

  private EncryptionOptions(boolean autoRevoke) {
    this(new InMemoryGroupKeyStore(), null, null, autoRevoke);
  }

  public GroupKeyStore getKeyStore() {
    return keyStore;
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
    return new EncryptionOptions();
  }
}
