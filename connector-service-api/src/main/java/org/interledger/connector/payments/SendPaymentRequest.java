package org.interledger.connector.payments;

import org.interledger.connector.accounts.AccountId;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

@Value.Immutable
public interface SendPaymentRequest {

  static ImmutableSendPaymentRequest.Builder builder() {
    return ImmutableSendPaymentRequest.builder();
  }

  AccountId accountId();

  UnsignedLong amount();

  String destinationPaymentPointer();

}
