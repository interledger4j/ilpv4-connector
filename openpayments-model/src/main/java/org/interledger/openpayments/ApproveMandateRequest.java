package org.interledger.openpayments;

import org.interledger.connector.accounts.AccountId;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Represents a request by the connector to send an XRP payment
 */
@Value.Immutable
@JsonSerialize(as = ImmutableApproveMandateRequest.class)
@JsonDeserialize(as = ImmutableApproveMandateRequest.class)
public interface ApproveMandateRequest {

  static ImmutableApproveMandateRequest.Builder builder() {
    return ImmutableApproveMandateRequest.builder();
  }

  AccountId accountId();

  MandateId mandateId();

  String memoToUser();

  Optional<HttpUrl> redirectUrl();

}
