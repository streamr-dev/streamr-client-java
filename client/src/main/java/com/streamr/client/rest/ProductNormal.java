package com.streamr.client.rest;

final class ProductNormal extends Product {
  private ProductNormal(final String name) {
    super(Type.NORMAL, name);
  }

  @Override
  public boolean equals(final Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return super.toString();
  }

  static class Builder extends Product.Builder<ProductNormal, ProductNormal.Builder> {
    Builder() {
      super(Builder.class);
    }

    public ProductNormal createProduct() {
      return new ProductNormal(name);
    }
  }
}
