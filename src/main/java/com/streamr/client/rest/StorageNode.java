package com.streamr.client.rest;

import com.streamr.client.utils.Address;

public class StorageNode {
    private Address address;

    public StorageNode(Address address) {
        this.address = address;
    }

    public Address getAddress() {
        return this.address;
    }
}