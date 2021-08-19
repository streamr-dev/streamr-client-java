package com.streamr.client.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.streamr.client.protocol.utils.RSAKeyPair;
import java.io.IOException;
import java.io.StringWriter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.Test;

class EncryptionOptionsTest {
  @Test
  void validatePrivateKeyThrowsOnInvalidPrivateKey() {
    Exception e =
        assertThrows(
            RuntimeException.class,
            () -> {
              EncryptionOptions.validatePrivateKey("wrong private key");
            });
    assertEquals("Must be a valid RSA private key in the PEM format.", e.getMessage());
  }

  @Test
  void validatePrivateKeyDoesNotThrowOnValidPrivateKey() throws IOException {
    RSAKeyPair keyPair = RSAKeyPair.create();
    byte[] key = keyPair.getPrivateKey().getEncoded();
    EncryptionOptions.validatePrivateKey(privateKeyToPem(key));
  }

  private String privateKeyToPem(byte[] key) throws IOException {
    StringWriter writer = new StringWriter();
    PemWriter pemWriter = new PemWriter(writer);
    try {
      pemWriter.writeObject(new PemObject("PRIVATE KEY", key));
      pemWriter.flush();
    } catch (IOException e) {
      throw e;
    } finally {
      try {
        pemWriter.close();
      } catch (IOException e) {
        throw e;
      }
    }
    return writer.toString();
  }
}
