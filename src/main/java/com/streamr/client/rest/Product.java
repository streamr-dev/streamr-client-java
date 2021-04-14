package com.streamr.client.rest;

/**
 * minimal Product object for testing
 */
public class Product {
    private String beneficiaryAddress;
    private String type;
    private int dataUnionVersion;

    public String getBeneficiaryAddress() {
        return beneficiaryAddress;
    }

    public void setBeneficiaryAddress(String beneficiaryAddress) {
        this.beneficiaryAddress = beneficiaryAddress;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getDataUnionVersion() {
        return dataUnionVersion;
    }

    public void setDataUnionVersion(int dataUnionVersion) {
        this.dataUnionVersion = dataUnionVersion;
    }
}
