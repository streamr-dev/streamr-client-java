package com.streamr.client.utils;

import com.streamr.client.java.util.Objects;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

/**
 * For making sure that Ethereum addresses are always treated similarly everywhere (e.g.
 * lower-cased)
 */
public class Address {
  private final String value;

  public Address(final byte[] value) {
    this(Numeric.toHexString(value));
  }

  public Address(final String value) {
    if ((value == null) || !WalletUtils.isValidAddress(value)) {
      throw new IllegalArgumentException("Invalid Ethereum address: " + value);
    }
    final String withPrefix = Numeric.prependHexPrefix(value);
    this.value = withPrefix.toLowerCase();
  }

  @Override
  public final boolean equals(final Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Address)) return false;
    final Address address1 = (Address) obj;
    return Objects.equals(value, address1.value);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public final String toString() {
    return this.value;
  }

  public final String toChecksumAddress() {
    return Keys.toChecksumAddress(this.value);
  }
}
