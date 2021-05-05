package com.streamr.client.dataunion;
/*

Uses RPC to get gas price, but not gasLimit.
ContractGasProvider is badly defined, doesn't align with RPC call eth_estimateGas()

This code is in PR but not yet included in Web3:
https://github.com/web3j/web3j/pull/973/files#diff-90867613c5138fd20f7eb93c98f0d879fb7f3df0913e8de2b468e96c76d5e3da

 * Copyright 2019 Web3 Labs LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import java.io.IOException;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;

public class EstimatedGasProvider implements ContractGasProvider {
  private static final Logger log = LoggerFactory.getLogger(EstimatedGasProvider.class);

  private final Web3j web3j;
  private BigInteger gasLimit;
  private BigInteger maxGasPrice;

  public EstimatedGasProvider(Web3j web3j, long maxGas) {
    this.web3j = web3j;
    this.gasLimit = BigInteger.valueOf(maxGas);
  }

  public void setMaxGasPrice(long max) {
    maxGasPrice = BigInteger.valueOf(max);
  }

  @Override
  public BigInteger getGasPrice(String contractFunc) {
    return getGasPrice();
  }

  @Override
  @Deprecated
  public BigInteger getGasPrice() {
    try {
      BigInteger currentGasPrice = web3j.ethGasPrice().send().getGasPrice();
      if (maxGasPrice != null && maxGasPrice.compareTo(currentGasPrice) < 0) {
        return maxGasPrice;
      }
      return currentGasPrice;
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public void setGasLimit(BigInteger limit) {
    gasLimit = limit;
  }

  @Override
  public BigInteger getGasLimit(String contractFunc) {
    return gasLimit;
  }

  @Override
  @Deprecated
  public BigInteger getGasLimit() {
    return gasLimit;
  }
}
