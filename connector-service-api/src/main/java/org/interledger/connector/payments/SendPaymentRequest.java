package org.interledger.connector.payments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.StreamConnectionDetails;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
public interface SendPaymentRequest {

  static ImmutableSendPaymentRequest.Builder builder() {
    return ImmutableSendPaymentRequest.builder();
  }

  AccountId accountId();

  UnsignedLong amount();

  /**
   * destination payment pointer. Must be set if streamConnectionDetails is empty.
   * @return
   */
  Optional<String> destinationPaymentPointer();


  /**
   * destination payment pointer. Must be set if destinationPaymentPointer is empty.
   * @return
   */
  Optional<StreamConnectionDetails> streamConnectionDetails();

  Optional<String> correlationId();

  @Value.Check
  default SendPaymentRequest validate() {
    Preconditions.checkArgument(destinationPaymentPointer().isPresent() || streamConnectionDetails().isPresent(),
      "one of destinationPaymentPointer or streamConnectionDetails must be set");
    Preconditions.checkArgument(!(destinationPaymentPointer().isPresent() && streamConnectionDetails().isPresent()),
      "only one of destinationPaymentPointer or streamConnectionDetails should be set, but not both");
    return this;
  }

}
