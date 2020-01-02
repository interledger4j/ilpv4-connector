package org.interledger.connector.balances;

import org.interledger.stream.Denomination;
import org.interledger.stream.ImmutableDenomination;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * API response for requests to get an account's balance
 */
@Value.Immutable
@JsonSerialize(as = ImmutableAccountBalanceResponse.class)
@JsonDeserialize(as = ImmutableAccountBalanceResponse.class)
public interface AccountBalanceResponse {


  static ImmutableAccountBalanceResponse.Builder builder() {
    return ImmutableAccountBalanceResponse.builder();
  }

  /**
   * Denomination configured  in {@link org.interledger.connector.accounts.AccountSettings} for this account
   * @return
   */
  @JsonSerialize(as = ImmutableDenomination.class)
  @JsonDeserialize(as = ImmutableDenomination.class)
  Denomination denomination();

  /**
   * Account balance
   *
   * @return
   */
  AccountBalance accountBalance();

}
