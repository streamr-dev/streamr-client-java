package com.streamr.client.rest;

import com.streamr.client.utils.Address;

import java.util.Objects;

public class StorageNode {
    private Address address;

    public StorageNode(Address address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageNode that = (StorageNode) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    public Address getAddress() {
        return this.address;
    }
}