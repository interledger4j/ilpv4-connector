package org.interledger.connector.accounts.sub;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A helper to centralize determination of sub-account related business logic.
 */
public interface SubAccountUtils {

  // The unique identifier of the account that collects all incoming ping protocol payments, if any.
  AccountId PING_ACCOUNT_ID = AccountId.of("__ping_account__");

  /**
   * The ILP address of the Connector that this code is being used with.
   *
   * @return A {@link Supplier} that resolves the current ILP address.
   */
  Supplier<InterledgerAddress> getConnectorOperatorAddress();

  /**
   * Determines if the supplied ILP destination address is a sub-account of this Connector. Sub-accounts can be...TODO!
   *
   * @param ilpAddress An {@link InterledgerAddress}.
   *
   * @return {@code true} if this account represents/routes-to a sub-account on this Connector; {@code false} otherwise.
   */
  default boolean isConnectorSubAccount(final InterledgerAddress ilpAddress) {
    Objects.requireNonNull(ilpAddress, "ilpAddress must not be null");
    return ilpAddress.startsWith(getConnectorOperatorAddress().get());
  }

  /**
   * An accessor for the ILP address of this Connector's Ping account.
   *
   * @return An {@link InterledgerAddress}.
   */
  default InterledgerAddress getPingAccountIlpAddress() {
    return getConnectorOperatorAddress().get().with(PING_ACCOUNT_ID.value());
  }

  /**
   * Determines if the supplied ILP destination address represents this Connector's Ping account.
   *
   * @param ilpAddress An {@link InterledgerAddress}.
   *
   * @return {@code true} if this account represents/routes-to a sub-account on this Connector; {@code false} otherwise.
   */
  default boolean isAddressForConnectorPingAccount(final InterledgerAddress ilpAddress) {
    Objects.requireNonNull(ilpAddress, "ilpAddress must not be null");
    return getPingAccountIlpAddress().equals(ilpAddress);
  }

  /**
   * Accessor for the identifier of this Connector's Ping account.
   *
   * @return An {@link AccountId} for the Ping account.
   */
  default AccountId getConnectorPingAccountId() {
    return PING_ACCOUNT_ID;
  }

  /**
   * Determines if the suppllied {@code accountId} is the Ping account.
   *
   * @param accountId {@code true} if the supplied accountId equals {@link #PING_ACCOUNT_ID}; {@code false} otherwise.
   *
   * @return An {@link AccountId} for the Ping account.
   */
  default boolean isConnectorPingAccountId(final AccountId accountId) {
    return PING_ACCOUNT_ID.equals(accountId);
  }
}
