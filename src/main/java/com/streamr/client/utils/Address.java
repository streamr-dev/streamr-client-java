package com.streamr.client.utils;

import org.apache.commons.codec.binary.Hex;

import java.util.Objects;

/**
 * For making sure that Ethereum addresses are always treated similarly everywhere (e.g. lower-cased)
 */
public class Address {
    private final String value;

    public Address(byte[] bytes) {
        this("0x" + Hex.encodeHexString(bytes));
    }

    // TODO: Use checksum case
    public Address(String value) {
        this.value = value.toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Address address1 = (Address) o;
        return Objects.equals(value, address1.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
