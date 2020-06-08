package org.interledger.connector.xumm.model;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.xumm.model.payload.TxJson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableXummPaymentRequest.class)
@JsonDeserialize(as = ImmutableXummPaymentRequest.class)
public interface XummPaymentRequest {

  static ImmutableXummPaymentRequest.Builder builder() {
    return ImmutableXummPaymentRequest.builder();
  }

  AccountId accountId();

  String paymentId();

  String memoToUser();

  TxJson txJson();

}
