package org.interledger.connector.accounts.sub;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.core.InterledgerAddress;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>When it comes to packet switching, this Connector supports four types of ILP address structure that is uses to
 * make packet routing determinations (Note these conventions are an implementation detail of the Connector because the
 * Connector both generates and parses these addresses without mandating any special functionality on intermediaries or
 * peers).</p>
 *
 * <p>The first type of address is the Connector operating address. Packets destined for this address will generally
 * be routed to the Ping account.</p>
 *
 * <p>The second type of address allows the Connector to locally-fulfill SPSP packets without needing to forward the
 * packet to an external SPSP server. This type of address is generated as follows: `{allocation-scheme}.{connector
 * -id}.{spsp-prefix}.{account-id}`. For example, if a Connector has an operating address of `example.connie` and an
 * spsp-prefix configured to be `spsp`, then any packets having a destination address starting with
 * `example.connie.spsp.alice` would be processed via the internal SPSP receiver.</p>
 *
 * <p>The third type of address allows the Connector to forward packets to Links (operating on behalf of accounts that
 * exist in this Connector) without having to consult the routing table. This type of address is generated as follows:
 * `{allocation-scheme}.{connector-id}.{accounts-prefix}.{account-id}`. For example, if a Connector has an operating
 * address of `example.connie` and an account-prefix configured to be `accounts`, then any packets having a destination
 * address starting with `example.connie.accounts.alice` will simply be forwarded to the Link defined by the `alice`
 * account. This allows alice to host sub-accounts of its own that it can forward or fulfill as needed.
 * </p>
 *
 * <p>The fourth type of address is an external address, which is defined as any address that does not start with the
 * `{allocation-scheme}.{connector-id}` of this Connector. For example, if a Connector has an operating address of
 * `example.connie`, then a packet with a destination address of `example.bob` would be routed according to the rules
 * defined in the routing table, which would typically be populated either by defining a static route, or via CCP route
 * updates.</p>
 */
public interface LocalDestinationAddressUtils {

  // The unique identifier of the account that collects all incoming ping protocol payments, if any.
  AccountId PING_ACCOUNT_ID = AccountId.of("__ping_account__");

  /**
   * The ILP address of the Connector that this code is being used with.
   *
   * @return A {@link Supplier} that resolves the current ILP address.
   */
  Supplier<InterledgerAddress> getConnectorOperatorAddress();

  /**
   * Determines if local SPSP fulfillment is enabled or disabled.
   *
   * @return {@code true} if local SPSP fulfillment is enabled; {@code false} otherwise.
   */
  boolean isLocalSpspFulfillmentEnabled();

  /**
   * An accessor for the ILP address of this Connector's Ping account.
   *
   * @return An {@link InterledgerAddress}.
   */
  default InterledgerAddress getPingAccountinterledgerAddress() {
    return getConnectorOperatorAddress().get();
  }

  /**
   * Accessor for the SPSP address prefix segment used by this Connector.
   *
   * @return An {@link String}.
   */
  default String getSpspAddressPrefixSegment() {
    return "spsp";
  }

  /**
   * Accessor for the local accounts address prefix segment used by this Connector.
   *
   * @return An {@link String}.
   */
  default String getLocalAccountsAddressPrefixSegment() {
    return "accounts";
  }

  /**
   * Determines if the supplied {@code interledgerAddress} represents a local account on this Connector.
   *
   * @param interledgerAddress An {@link InterledgerAddress}.
   *
   * @return {@code true} if this account represents/routes-to a sub-account on this Connector; {@code false} otherwise.
   */
  default boolean isLocalDestinationAddress(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null");
    return interledgerAddress.startsWith(getConnectorOperatorAddress().get());
  }

  /**
   * Determines if the supplied ILP destination address represents this Connector's Ping account.
   *
   * @param interledgerAddress An {@link InterledgerAddress}.
   *
   * @return {@code true} if this account represents/routes-to a sub-account on this Connector; {@code false} otherwise.
   */
  default boolean isAddressForConnectorPingAccount(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null");
    return getPingAccountinterledgerAddress().equals(interledgerAddress);
  }

  /**
   * Determines if the supplied ILP destination address is an SPSP sub-account of this Connector.
   *
   * @param interledgerAddress An {@link InterledgerAddress}.
   *
   * @return {@code true} if this account represents/routes-to a sub-account on this Connector; {@code false} otherwise.
   */
  default boolean isLocalSpspDestinationAddress(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null");
    return isLocalSpspFulfillmentEnabled() && interledgerAddress
      .startsWith(getConnectorOperatorAddress().get().with(getSpspAddressPrefixSegment()));
  }

  /**
   * Determines if the supplied ILP destination address is a local account of this Connector.
   *
   * @param interledgerAddress An {@link InterledgerAddress}.
   *
   * @return {@code true} if this account represents/routes-to a sub-account on this Connector; {@code false} otherwise.
   */
  default boolean isLocalAccountDestinationAddress(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null");
    return isLocalSpspFulfillmentEnabled() && interledgerAddress
      .startsWith(getConnectorOperatorAddress().get().with(getLocalAccountsAddressPrefixSegment()));
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
    Objects.requireNonNull(accountId, "accountId must not be null");
    return PING_ACCOUNT_ID.equals(accountId);
  }

  /**
   * Given an {@link InterledgerAddress}, parse out the segment that corresponds to the SPSP Account identifier. For
   * example, given a Connector operating address of `g.connector` and an SPSP prefix segment of `accounts`, then
   * locally fulfilled SPSP payments should be addressed to a destination starting with
   * `g.connector.accounts.{accountId}`. In this case, the `{accountId}` is the value returned by this method.
   *
   * @param interledgerAddress An {@link InterledgerAddress} to parse.
   *
   * @return An {@link AccountId} for the supplied address.
   */
  default AccountId parseSpspAccountId(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null!");

    //g.connector.accounts.bob.123xyz --> bob
    //g.connector.accounts.bob~foo.bar --> bob~foo
    //g.connector.accounts.bob.~foo --> bob
    //g.connector.accounts.~bob.bar --> bob

    // Strip off spspAddress
    final String withoutOperatorAddress = StringUtils.substringAfter(
      interledgerAddress.getValue(), getConnectorOperatorAddress().get().getValue() + "."
    );
    final String withoutSpsp = StringUtils.substringAfter(withoutOperatorAddress, getSpspAddressPrefixSegment() + ".");
    final String accountIdString = StringUtils.substringBefore(withoutSpsp, ".");

    if (accountIdString.isEmpty()) {
      throw new AccountNotFoundProblem(
        "Invalid SPSP accountId parsed from interledgerAddress=" + interledgerAddress, AccountId.of(accountIdString)
      );
    } else {
      return AccountId.of(accountIdString);
    }
  }

  /**
   * Given an {@link InterledgerAddress}, parse out the segment that corresponds to the SPSP Account identifier. For
   * example, given a Connector operating address of `g.connector`, then locally fulfilled SPSP payments should be
   * addresses to something like `g.connector.{accountId}.{spsp_info}`. In this case, the `{accountId}` is the value
   * returned by this method.
   *
   * @param interledgerAddress An {@link InterledgerAddress} to parse.
   *
   * @return An {@link AccountId} for the supplied address.
   */
  default AccountId parseLocalAccountId(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null!");

    //g.connector.accounts.bob.123xyz --> bob
    //g.connector.accounts.bob~foo.bar --> bob~foo
    //g.connector.accounts.bob.~foo --> bob
    //g.connector.accounts.~bob.bar --> bob

    final String withoutOperatorAddress = StringUtils.substringAfter(
      interledgerAddress.getValue(), getConnectorOperatorAddress().get().getValue() + "."
    );
    final String withoutAccountsPrefix = StringUtils.substringAfter(
      withoutOperatorAddress, getLocalAccountsAddressPrefixSegment() + "."
    );
    final String accountIdString = StringUtils.substringBefore(withoutAccountsPrefix, ".");

    if (accountIdString.isEmpty()) {
      throw new AccountNotFoundProblem(
        "Invalid local accountId parsed from interledgerAddress=" + interledgerAddress, AccountId.of(accountIdString)
      );
    } else {
      return AccountId.of(accountIdString);
    }


  }
}
