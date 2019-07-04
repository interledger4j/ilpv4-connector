package org.interledger.ilpv4.connector.settlement;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementService;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.ilpv4.connector.core.settlement.Quantity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;

import java.util.Objects;
import java.util.UUID;

/**
 * The default implementation of {@link SettlementService}.
 */
public class DefaultSettlementService implements SettlementService {

  private final AccountSettingsRepository accountSettingsRepository;
  private final BalanceTracker balanceTracker;

  public DefaultSettlementService(
    final AccountSettingsRepository accountSettingsRepository, final BalanceTracker balanceTracker
  ) {
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.balanceTracker = Objects.requireNonNull(balanceTracker);
  }

  @Override
  public Quantity handleIncomingSettlement(UUID idempotencyKey, AccountId accountId, Quantity incomingSettlement) {

    final AccountSettings accountSettings = this.accountSettingsRepository
      .findByAccountId(accountId)
      .orElseThrow(() -> new AccountNotFoundProblem(accountId));

    // Determine the normalized Quantity (i.e., translate from Settlement Ledger units to ILP Clearing Ledger
    // units
    final Quantity quantityToAdjustInClearingLayer =
      NumberScalingUtils.translate(incomingSettlement, accountSettings.getAssetScale());

    // Update the balance in the clearing layer based upon what was settled.
    this.balanceTracker.updateBalanceForSettlement(idempotencyKey, accountId, quantityToAdjustInClearingLayer);

    return quantityToAdjustInClearingLayer;
  }
}
