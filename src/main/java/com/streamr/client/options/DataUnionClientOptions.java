package com.streamr.client.options;

public class DataUnionClientOptions {

    private String dataUnionSidechainFactoryAddress = "0x1b55587Beea0b5Bc96Bb2ADa56bD692870522e9f";
    private String dataUnionMainnetFactoryAddress = "0x7d55f9981d4E10A193314E001b96f72FCc901e40";
    private String binanceAdapterAddress = "0x0";
    private String mainnetAdminPrvKey;
    private String sidechainAdminPrvKey;

    public DataUnionClientOptions(String mainnetAdminPrvKey, String sidechainAdminPrvKey){
        this.mainnetAdminPrvKey = mainnetAdminPrvKey;
        this.sidechainAdminPrvKey = sidechainAdminPrvKey;
    }

    public String getDataUnionSidechainFactoryAddress() {
        return dataUnionSidechainFactoryAddress;
    }

    public void setDataUnionSidechainFactoryAddress(String dataUnionSidechainFactoryAddress) {
        this.dataUnionSidechainFactoryAddress = dataUnionSidechainFactoryAddress;
    }

    public String getDataUnionMainnetFactoryAddress() {
        return dataUnionMainnetFactoryAddress;
    }

    public void setDataUnionMainnetFactoryAddress(String dataUnionMainnetFactoryAddress) {
        this.dataUnionMainnetFactoryAddress = dataUnionMainnetFactoryAddress;
    }
    public String getBinanceAdapterAddress() {
        return binanceAdapterAddress;
    }

    public void setBinanceAdapterAddress(String binanceAdapaterAddress) {
        this.binanceAdapterAddress = binanceAdapaterAddress;
    }

    public String getSidechainAdminPrvKey() {
        return sidechainAdminPrvKey;
    }

    public void setSidechainAdminPrvKey(String sidechainAdminPrvKey) {
        this.sidechainAdminPrvKey = sidechainAdminPrvKey;
    }

    public String getMainnetAdminPrvKey() {
        return mainnetAdminPrvKey;
    }

    public void setMainnetAdminPrvKey(String mainnetAdminPrvKey) {
        this.mainnetAdminPrvKey = mainnetAdminPrvKey;
    }
}
