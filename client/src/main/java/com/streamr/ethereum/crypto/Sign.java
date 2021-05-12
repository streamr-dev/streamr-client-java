package com.streamr.ethereum.crypto;

import com.streamr.ethereum.common.Address;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

public final class Sign {
    private Sign() {}

    public static boolean verify(String data, String signature, Address address)
        throws SignatureException {
      byte[] messageHash = org.web3j.crypto.Sign.getEthereumMessageHash(data.getBytes());
      Address recovered = recoverAddress(messageHash, signature);
      return recovered.equals(address);
    }

    public static Address recoverAddress(byte[] messageHash, String signatureHex)
        throws SignatureException {
      byte[] source = Numeric.hexStringToByteArray(signatureHex);
      byte v = source[64];
      if (v < 27) {
        v += 27;
      }
      byte[] r = Arrays.copyOfRange(source, 0, 32);
      byte[] s = Arrays.copyOfRange(source, 32, 64);
      org.web3j.crypto.Sign.SignatureData signature = new org.web3j.crypto.Sign.SignatureData(v, r, s);
      for (byte i = 0; i < 4; i++) {
        BigInteger publicKey;
        try {
          publicKey = org.web3j.crypto.Sign.signedMessageHashToKey(messageHash, signature);
        } catch (SignatureException e) {
          continue;
        }
        if (publicKey != null) {
          String hex = Keys.getAddress(publicKey);
          return new Address(hex);
        }
      }
      throw new SignatureException("Address recovery from signature failed.");
    }

    public static String sign(final BigInteger privateKey, final String data) {
      final ECKeyPair account = ECKeyPair.create(privateKey);
      byte[] msg = data.getBytes();
      org.web3j.crypto.Sign.SignatureData sign = org.web3j.crypto.Sign.signPrefixedMessage(msg, account);
      byte[] result = new byte[65];
      System.arraycopy(sign.getR(), 0, result, 0, 32);
      System.arraycopy(sign.getS(), 0, result, 32, 32);
      System.arraycopy(sign.getV(), 0, result, 64, 1);
      return Numeric.toHexString(result);
    }
}
