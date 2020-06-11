package org.interledger.connector.xumm.model;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.core.immutables.Wrapped;
import org.interledger.openpayments.ApproveMandateRequest;
import org.interledger.openpayments.SendXrpPaymentRequest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
public abstract class PayloadBlobWrapper<T> {

  @Value.Parameter
  public abstract AccountId accountId();

  @Value.Parameter
  public abstract T value();

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + value() + ")";
  }

  @Immutable
  @Wrapped
  @JsonSerialize(as = SendXrpRequestWrapper.class)
  @JsonDeserialize(as = SendXrpRequestWrapper.class)
  static abstract class _SendXrpRequestWrapper extends PayloadBlobWrapper<SendXrpPaymentRequest> {}

  @Immutable
  @Wrapped
  @JsonSerialize(as = ApproveMandateRequestWrapper.class)
  @JsonDeserialize(as = ApproveMandateRequestWrapper.class)
  static abstract class _ApproveMandateRequestWrapper extends PayloadBlobWrapper<ApproveMandateRequest> {}

}
