package org.interledger.connector.transactions;

public enum TransactionType {

  PAYMENT_RECEIVED(BalanceAdjustmentType.CREDIT),
  PAYMENT_SENT(BalanceAdjustmentType.DEBIT),
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
