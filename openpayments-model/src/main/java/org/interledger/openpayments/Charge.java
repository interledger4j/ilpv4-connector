package org.interledger.openpayments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableCharge.class)
@JsonDeserialize(as = ImmutableCharge.class)
public interface Charge extends NewCharge {

  static ImmutableCharge.Builder builder() {
    return ImmutableCharge.builder();
  }

  HttpUrl id();

  HttpUrl mandate();

  ChargeStatus status();

  UnsignedLong amount();

  ChargeId chargeId();

  MandateId mandateId();

  Optional<String> userAuthorizationUrl();

}
