package com.streamr.client.options;

public class DataUnionClientOptions {

    // TODO: read these from configs, see ETH-193
    //mainnet - xDai addresses are default:
    private String dataUnionSidechainFactoryAddress = "0x1b55587Beea0b5Bc96Bb2ADa56bD692870522e9f";
    private String dataUnionMainnetFactoryAddress = "0x7d55f9981d4E10A193314E001b96f72FCc901e40";
    private String binanceAdapterAddress = "0x0c1aF6edA561fbDA48E9A7B1Dd46D216F31A97cC";
    private String binanceSmartChainAMBAddress = "0x05185872898b6f94aa600177ef41b9334b1fa48b";
    private String mainnetAdminPrivateKey;
    private String sidechainAdminPrivateKey;
    private String binanceRPC = "https://bsc-dataseed.binance.org/";
    private String mainnetRPC;
    private String sidechainRPC;
    private String withdrawServerBaseUrl = "http://localhost:3000";
    private long connectionTimeoutMillis = 60000;

    public DataUnionClientOptions(String mainnetRPC, String mainnetAdminPrivateKey, String sidechainRPC, String sidechainAdminPrivateKey){
        this.mainnetRPC = mainnetRPC;
        this.mainnetAdminPrivateKey = mainnetAdminPrivateKey;
        this.sidechainRPC = sidechainRPC;
        this.sidechainAdminPrivateKey = sidechainAdminPrivateKey;
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

    public String getSidechainAdminPrivateKey() {
        return sidechainAdminPrivateKey;
    }

    public void setSidechainAdminPrivateKey(String sidechainAdminPrivateKey) {
        this.sidechainAdminPrivateKey = sidechainAdminPrivateKey;
    }

    public String getMainnetAdminPrivateKey() {
        return mainnetAdminPrivateKey;
    }

    public void setMainnetAdminPrivateKey(String mainnetAdminPrivateKey) {
        this.mainnetAdminPrivateKey = mainnetAdminPrivateKey;
    }

    public String getBinanceSmartChainAMBAddress() {
        return binanceSmartChainAMBAddress;
    }

    public void setBinanceSmartChainAMBAddress(String binanceSmartChainAMBAddress) {
        this.binanceSmartChainAMBAddress = binanceSmartChainAMBAddress;
    }

    public String getBinanceRPC() {
        return binanceRPC;
    }

    public void setBinanceRPC(String binanceRPC) {
        this.binanceRPC = binanceRPC;
    }

    public String getMainnetRPC() {
        return mainnetRPC;
    }

    public void setMainnetRPC(String mainnetRPC) {
        this.mainnetRPC = mainnetRPC;
    }

    public String getSidechainRPC() {
        return sidechainRPC;
    }

    public void setSidechainRPC(String sidechainRPC) {
        this.sidechainRPC = sidechainRPC;
    }

    public String getWithdrawServerBaseUrl() {
        return withdrawServerBaseUrl;
    }

    public void setWithdrawServerBaseUrl(String withdrawServerBaseUrl) {
        this.withdrawServerBaseUrl = withdrawServerBaseUrl;
    }

    public long getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public void setConnectionTimeoutMillis(long connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }
}
