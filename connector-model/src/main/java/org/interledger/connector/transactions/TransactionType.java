package org.interledger.connector.transactions;

/**
 * Types of {@link Transaction} that the system tracks.
 */
public enum TransactionType {

  PAYMENT_RECEIVED(BalanceAdjustmentType.CREDIT),
  PAYMENT_SENT(BalanceAdjustmentType.DEBIT),
  // for raw status strings from the database that don't map to any of the above. Shouldn't happen but just in case
  // map to this rather than null
  UNKNOWN(null);

  private BalanceAdjustmentType adjustmentType;

  TransactionType(BalanceAdjustmentType adjustmentType) {
    this.adjustmentType = adjustmentType;
  }

  public BalanceAdjustmentType getAdjustmentType() {
    return adjustmentType;
  }

  /**
   * Types of changes that a transaction can make to a balance
   */
  enum BalanceAdjustmentType {
    DEBIT,
    CREDIT
  }

}
