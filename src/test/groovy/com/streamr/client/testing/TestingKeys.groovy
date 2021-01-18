package com.streamr.client.testing

import org.web3j.utils.Numeric

final class TestingKeys {
    private TestingKeys() {}

    public static String generatePrivateKey() {
        byte[] array = new byte[32]
        new Random().nextBytes(array)
        return Numeric.toHexString(array)
    }
}
