package org.interledger.openpayments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

/**
 * Not 2 dudes going out for burgers and beer.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableMandate.class)
@JsonDeserialize(as = ImmutableMandate.class)
public interface Mandate extends NewMandate {

  static ImmutableMandate.Builder builder() {
    return ImmutableMandate.builder();
  }

  MandateId mandateId();

  String accountId();

  HttpUrl id();

  HttpUrl account();

  MandateStatus status();

  UnsignedLong balance();

  List<Charge> charges();

  Optional<HttpUrl> userAuthorizationUrl();

  default UnsignedLong totalCharged() {
    return charges().stream()
      .filter(charge -> !charge.status().equals(ChargeStatus.PAYMENT_FAILED))
      .map(Charge::amount)
      .reduce(UnsignedLong.ZERO, UnsignedLong::plus);
  }

}
