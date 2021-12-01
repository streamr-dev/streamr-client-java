package com.streamr.client.options;

public class DataUnionClientOptions {

    // TODO: read these from configs, see ETH-193
    //mainnet - xDai addresses are default:
    private String dataUnionSidechainFactoryAddress = "0xFCE1FBFAaE61861B011B379442c8eE1DC868ABd0";
    private String dataUnionMainnetFactoryAddress = "0xE41439BF434F9CfBF0153f5231C205d4ae0C22e3";
    private String binanceAdapterAddress = "0x193888692673b5dD46e6BC90bA8cBFeDa515c8C1";
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
