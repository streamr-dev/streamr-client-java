package com.streamr.client.rest;

import com.streamr.client.java.util.Objects;

final class ProductDataUnion extends Product {
  private final String beneficiaryAddress;
  private final int dataUnionVersion;

  private ProductDataUnion(
      final String name, final String beneficiaryAddress, final int dataUnionVersion) {
    super(Type.DATAUNION, name);
    Objects.requireNonNull(beneficiaryAddress, "beneficiaryAddress");
    this.beneficiaryAddress = beneficiaryAddress;
    Objects.requireNonNull(dataUnionVersion, "dataUnionVersion");
    this.dataUnionVersion = dataUnionVersion;
  }

  public String getBeneficiaryAddress() {
    return beneficiaryAddress;
  }

  public int getDataUnionVersion() {
    return dataUnionVersion;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    if (!super.equals(obj)) return false;
    final ProductDataUnion that = (ProductDataUnion) obj;
    return getType() == that.getType()
        && Objects.equals(getName(), that.getName())
        && dataUnionVersion == that.dataUnionVersion
        && Objects.equals(beneficiaryAddress, that.beneficiaryAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), beneficiaryAddress, dataUnionVersion);
  }

  @Override
  public String toString() {
    return String.format(
        "ProductDataUnion{type=%s, name='%s', beneficiaryAddress='%s', dataUnionVersion=%d}",
        getType(), getName(), beneficiaryAddress, dataUnionVersion);
  }

  public static class Builder extends Product.Builder<ProductDataUnion, ProductDataUnion.Builder> {
    private static final int DEFAULT_DATA_UNION_VERSION = 2;
    private String beneficiaryAddress;

    public Builder() {
      super(Builder.class);
    }

    public ProductDataUnion.Builder withBeneficiaryAddress(final String beneficiaryAddress) {
      this.beneficiaryAddress = beneficiaryAddress;
      return this;
    }

    public ProductDataUnion createProduct() {
      return new ProductDataUnion(name, beneficiaryAddress, DEFAULT_DATA_UNION_VERSION);
    }
  }
}
