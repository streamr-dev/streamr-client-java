package com.streamr.client.rest;

import com.streamr.client.java.util.Objects;

abstract class Product {
  enum Type {
    NORMAL,
    DATAUNION
  }

  private final Type type;
  private final String name;

  protected Product(final Type type, final String name) {
    Objects.requireNonNull(type, "type");
    this.type = type;
    Objects.requireNonNull(name, "name");
    this.name = name;
  }

  public Type getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final Product product = (Product) obj;
    return type == product.type && Objects.equals(name, product.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, name);
  }

  protected abstract static class Builder<T extends Product, B extends Product.Builder<T, B>> {
    protected Type type;
    protected String name;
    protected Class<B> klass;

    protected Builder(final Class<B> klass) {
      Objects.requireNonNull(klass, "klass");
      this.klass = klass;
    }

    public B withType(final Type type) {
      this.type = type;
      return klass.cast(this);
    }

    public B withName(final String name) {
      this.name = name;
      return klass.cast(this);
    }

    public abstract T createProduct();
  }
}
