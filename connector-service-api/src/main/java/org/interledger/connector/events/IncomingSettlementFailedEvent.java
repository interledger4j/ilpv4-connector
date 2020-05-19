package org.interledger.connector.events;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.core.settlement.SettlementQuantity;
import org.interledger.connector.settlement.SettlementServiceException;
import org.interledger.core.Immutable;

import org.immutables.value.Value.Derived;

import java.util.Optional;

/**
 * Indicates a local settlement was processed by this Connector's Settlement Engine and propagated to this Connector via
 * the `/accounts/{accountId}/settlements` endpoint.
 */
@Immutable
public interface IncomingSettlementFailedEvent extends ConnectorEvent {

  static IncomingSettlementFailedEventBuilder builder() {
    return new IncomingSettlementFailedEventBuilder();
  }

  /**
   * The AccountId that this settlement request failure pertains to.
   *
   * @return If the AccountSettings is present in this class, then returns {@link AccountSettings#accountId()}.
   *   Otherwise, returns the settlement engine accountId as an {@link AccountId}.
   */
  @Derived
  default AccountId requestedAccountId() {
    return accountSettings()
      .map(AccountSettings::accountId)
      .orElseGet(() -> AccountId.of(settlementEngineAccountId().value()));
  }

  /**
   * The idempotency key used for retrying requests. Defined via IL-RFC-38.
   *
   * @return A {@link String}.
   */
  String idempotencyKey();

  /**
   * The unique identifier of the settlement engine account that sent an incoming settlement to this Connector.
   *
   * @return A {@link SettlementEngineAccountId}.
   */
  SettlementEngineAccountId settlementEngineAccountId();

  /**
   * The number of units in the associated settlement request that were originally requested to be settled for,
   * denominated in the settlement engine's units.
   *
   * @return A {@link SettlementQuantity}.
   */
  SettlementQuantity incomingSettlementInSettlementUnits();

  /**
   * The {@link SettlementServiceException} that triggered the settlement failure.
   *
   * @return A {@link SettlementServiceException}.
   */
  SettlementServiceException settlementServiceException();

  @Override
  default Optional<String> message() {
    return Optional.of("Local settlement failed.");
  }

}
