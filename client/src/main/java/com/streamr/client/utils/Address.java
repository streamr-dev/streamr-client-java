package com.streamr.client.utils;

import com.streamr.client.java.util.Objects;
import org.web3j.utils.Numeric;

/**
 * For making sure that Ethereum addresses are always treated similarly everywhere (e.g.
 * lower-cased)
 */
public class Address {
  private final String value;

  public Address(final byte[] value) {
    final String withoutPrefix = Numeric.toHexString(value);
    final String withPrefix = Numeric.prependHexPrefix(withoutPrefix);
    this.value = withPrefix.toLowerCase();
  }

  public Address(final String value) {
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
    return value;
  }
}
