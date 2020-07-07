package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyException;
import com.streamr.client.exceptions.UnableToDecryptException;
import org.apache.commons.codec.binary.Hex;

import java.util.Date;
import java.util.Map;

public class EncryptedGroupKey extends GroupKey {
    public EncryptedGroupKey(String groupKeyHex, Date start) {
        super(groupKeyHex, start);
    }

    public UnencryptedGroupKey getDecrypted(EncryptionUtil encryptionUtil) throws UnableToDecryptException, InvalidGroupKeyException {
        String plaintext = Hex.encodeHexString(encryptionUtil.decryptWithPrivateKey(groupKeyHex));
        return new UnencryptedGroupKey(plaintext, start);
    }

}
