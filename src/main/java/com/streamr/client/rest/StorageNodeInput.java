package com.streamr.client.rest;

import com.streamr.client.utils.Address;

public final class StorageNodeInput {
    private final Address storageNodeAddress;
    public StorageNodeInput(Address storageNodeAddress) {
        this.storageNodeAddress = storageNodeAddress;
    }

    public Address getStorageNodeAddress() {
        return storageNodeAddress;
    }
}
