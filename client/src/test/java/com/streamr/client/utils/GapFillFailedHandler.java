package com.streamr.client.utils;

import java.util.function.Function;

public class GapFillFailedHandler implements Function<GapFillFailedException, Void> {
  @Override
  public Void apply(final GapFillFailedException e) {
    throw e;
  }
}
