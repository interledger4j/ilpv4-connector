package org.interledger.connector.opa.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableCreateInvoiceRequest.class)
@JsonDeserialize(as = ImmutableCreateInvoiceRequest.class)
public interface CreateInvoiceRequest {

  static ImmutableCreateInvoiceRequest.Builder builder() {
    return ImmutableCreateInvoiceRequest.builder();
  }

  String subject();

  UnsignedLong amount();

  String assetCode();

  short assetScale();

  Optional<String> description();

}
