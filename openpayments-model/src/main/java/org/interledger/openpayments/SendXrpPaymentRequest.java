package org.interledger.openpayments;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Represents a request by the connector to send an XRP payment
 */
@Value.Immutable
public interface SendXrpPaymentRequest {

  static ImmutableSendXrpPaymentRequest.Builder builder() {
    return ImmutableSendXrpPaymentRequest.builder();
  }

  String accountId();

  String correlationId();

  Optional<String> instructionsToUser();

  String destinationAddress();

  Optional<Integer> destinationTag();

  UnsignedLong amountInDrops();

}
