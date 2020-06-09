package org.interledger.openpayments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.util.Optional;

@JsonSerialize(as = ImmutableNewCharge.class)
@JsonDeserialize(as = ImmutableNewCharge.class)
public interface NewCharge {

  static ImmutableNewCharge.Builder builder() {
    return ImmutableNewCharge.builder();
  }

  HttpUrl invoice();

  Optional<String> paymentCompleteRedirectUrl();

  @Value.Immutable
  abstract class AbstractNewCharge implements NewCharge {

  }

}
