package com.sappenin.interledger.ilpv4.connector.settlement;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceTrackerException;
import org.interledger.connector.accounts.AccountId;
import org.interledger.ilpv4.connector.core.settlement.Quantity;

import java.util.UUID;

/**
 * A service for handling interactions with the Settlement Service that allows a separation of HTTP caching and
 * idempotence from actual settlement logic.
 */
public interface SettlementService {

  /**
   * Handles an incoming Settlement message (these are generally send into a Connector from an external process called a
   * Settlement Engine).
   *
   * @param idempotencyKey     A unique key to ensure idempotent requests.
   * @param accountId
   * @param incomingSettlement
   *
   * @return A new {@link Quantity} that indicates how much was settled innside of this Connector's ILP clearing layer.
   *
   * @throws BalanceTrackerException If anything goes wrong while attempting to update the clearingBalance.
   */
  Quantity handleIncomingSettlement(
    UUID idempotencyKey, AccountId accountId, Quantity incomingSettlement
  ) throws BalanceTrackerException;


}
