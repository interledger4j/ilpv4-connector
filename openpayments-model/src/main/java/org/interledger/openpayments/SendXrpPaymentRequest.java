package org.interledger.openpayments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Represents a request by the connector to send an XRP payment
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSendXrpPaymentRequest.class)
@JsonDeserialize(as = ImmutableSendXrpPaymentRequest.class)
public interface SendXrpPaymentRequest {

  static ImmutableSendXrpPaymentRequest.Builder builder() {
    return ImmutableSendXrpPaymentRequest.builder();
  }

  String correlationId();

  String destinationAddress();

  Optional<Integer> destinationTag();

  UnsignedLong amountInDrops();

}
