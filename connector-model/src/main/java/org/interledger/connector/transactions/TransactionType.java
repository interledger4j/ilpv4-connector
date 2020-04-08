package org.interledger.connector.transactions;

/**
 * Types of {@link Transaction} that the system tracks.
 */
public enum TransactionType {

  PAYMENT_RECEIVED(BalanceAdjustmentType.CREDIT),
  PAYMENT_SENT(BalanceAdjustmentType.DEBIT);

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
