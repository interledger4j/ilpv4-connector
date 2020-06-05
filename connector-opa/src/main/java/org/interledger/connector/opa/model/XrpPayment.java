package org.interledger.connector.opa.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableXrpPayment.class)
@JsonDeserialize(as = ImmutableXrpPayment.class)
public interface XrpPayment extends AuthorizablePayment {

  static ImmutableXrpPayment.Builder builder() {
    return ImmutableXrpPayment.builder();
  }

  Optional<XrplTransaction> transaction();


}
