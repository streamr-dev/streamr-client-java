package com.streamr.client.rest;

public class SetBinanceRecipientFromSignature {
    String memberAddress;
    String binanceRecipientAddress;
    String signature;
    public SetBinanceRecipientFromSignature(String memberAddress, String binanceRecipientAddress, String signature) {
        this.memberAddress = memberAddress;
        this.binanceRecipientAddress = binanceRecipientAddress;
        this.signature = signature;
    }
}
