package com.streamr.client.utils;

import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.message_layer.StreamMessage;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;

public class DecryptionKeySequence {
    private ArrayList<SecretKey> keys = new ArrayList<>();
    private int currentIndex = 0;

    public DecryptionKeySequence(ArrayList<GroupKey> keys) {
        for (GroupKey k: keys) {
            this.keys.add(new SecretKeySpec(DatatypeConverter.parseHexBinary(k.getGroupKeyHex()), "AES"));
        }
    }

    public void tryToDecryptResent(StreamMessage msg) {
        try {
            EncryptionUtil.decryptStreamMessage(msg, keys.get(currentIndex));
        } catch (UnableToDecryptException e) {
            // the current key might not be valid anymore
            SecretKey nextKey = getNextKey();
            if (nextKey == null) {
                throw e;
            }
            // try to decrypt with the next key
            EncryptionUtil.decryptStreamMessage(msg, nextKey);
            // if successful (no error thrown), update the current key
            currentIndex++;
        }
    }

    private SecretKey getNextKey() {
        if (currentIndex == keys.size() - 1) {
            return null;
        }
        return keys.get(currentIndex + 1);
    }
}
