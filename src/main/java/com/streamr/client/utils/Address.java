package com.streamr.client.utils;

import org.apache.commons.codec.binary.Hex;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;

import java.util.Random;

import java.util.Random;

/**
 * For making sure that Ethereum addresses are always treated similarly everywhere (e.g. lower-cased)
 */
public class Address {
    private final String address;

    public Address(byte[] bytes) {
        this("0x" + Hex.encodeHexString(bytes));
    }

    public Address(String address) {
        if ((address == null) || !WalletUtils.isValidAddress(address)) {
            throw new IllegalArgumentException("Invalid Ethereum address: " + address);
        }
        this.address = address.toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Address address1 = (Address) o;

        return address.equals(address1.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public String toString() {
        return Keys.toChecksumAddress(this.address);
    }

    public String toLowerCaseString() {
        return this.address;
    }

    public static Address createRandom() {
        byte[] array = new byte[20];
        new Random().nextBytes(array);
        return new Address(array);
    }
}
