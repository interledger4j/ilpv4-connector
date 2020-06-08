package org.interledger.openpayments;

import okhttp3.HttpUrl;

import java.util.Objects;

public interface PayIdResolver {

  static PayIdResolver defaultPayIdResolver() {
    return new DefaultPayIdResolver();
  }

  HttpUrl resolveHttpUrl(PayId payId);

  class DefaultPayIdResolver implements PayIdResolver {

    @Override
    public HttpUrl resolveHttpUrl(PayId payId) {
      Objects.requireNonNull(payId);
      return HttpUrl.parse("https://" + payId.host() + "/" + payId.account());
    }
  }
}
