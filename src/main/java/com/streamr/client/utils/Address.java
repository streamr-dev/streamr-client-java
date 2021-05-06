package com.streamr.client.utils;

import java.util.Objects;
import java.util.Random;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

/**
 * For making sure that Ethereum addresses are always treated similarly everywhere (e.g. lower-cased)
 */
public class Address {
    private final String address;

    public Address(byte[] bytes) {
        this(Numeric.toHexString(bytes));
    }

    public Address(String address) {
        if ((address == null) || !WalletUtils.isValidAddress(address)) {
            throw new IllegalArgumentException("Invalid Ethereum address: " + address);
        }
        this.address = address.toLowerCase();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Address)) return false;
        final Address address1 = (Address) obj;
        return Objects.equals(address, address1.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
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
