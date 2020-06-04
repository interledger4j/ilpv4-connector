package org.interledger.openpayments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableNewInvoice.class)
@JsonDeserialize(as = ImmutableNewInvoice.class)
public interface NewInvoice {

  static ImmutableNewInvoice.Builder builder() {
    return ImmutableNewInvoice.builder();
  }

  UnsignedLong amount();

  String subject();

  String assetCode();

  short assetScale();

  String description();

}

