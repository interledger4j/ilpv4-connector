package org.interledger.connector.server.spring.controllers.pay;

import org.interledger.connector.payments.StreamPayment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.math.BigInteger;

/**
 * Extension of {@link StreamPayment} with derived fields for API backwards compatibility with Xpring4J SDK.
 * This only applies to payments that were sent via local send and does not make sense for received payments
 * or payments that were sent via external ILP client.
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableLocalSendPaymentResponse.class)
@JsonSerialize(as = ImmutableLocalSendPaymentResponse.class)
public interface LocalSendPaymentResponse extends StreamPayment {

  static ImmutableLocalSendPaymentResponse.Builder builder() {
    return ImmutableLocalSendPaymentResponse.builder();
  }

  @Value.Derived
  default UnsignedLong originalAmount() {
    return this.expectedAmount().map(BigInteger::negate)
      .map(BigInteger::longValue)
      .map(UnsignedLong::valueOf)
      .orElse(UnsignedLong.ZERO);
  }

  @Value.Derived
  default UnsignedLong amountDelivered() {
    return this.deliveredAmount();
  }

  @Value.Derived
  default UnsignedLong amountSent() {
    return UnsignedLong.valueOf(this.amount().negate());
  }

  @Value.Derived
  default boolean successfulPayment() {
    return this.expectedAmount().map(expected -> expected.equals(this.amount())).orElse(false);
  }

  @Value.Check
  default LocalSendPaymentResponse validate() {
    return this;
  }

}
