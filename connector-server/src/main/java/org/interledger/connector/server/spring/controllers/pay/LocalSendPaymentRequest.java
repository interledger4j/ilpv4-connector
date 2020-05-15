package org.interledger.connector.server.spring.controllers.pay;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * API request object for a request to initiate a local send from the connector
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableLocalSendPaymentRequest.class)
@JsonSerialize(as = ImmutableLocalSendPaymentRequest.class)
public interface LocalSendPaymentRequest {

  static ImmutableLocalSendPaymentRequest.Builder builder() {
    return ImmutableLocalSendPaymentRequest.builder();
  }

  String destinationPaymentPointer();
  UnsignedLong amount();
  Optional<String> correlationId();

}
