package org.interledger.connector.payments;

/**
 * Types of {@link StreamPayment} that the system tracks.
 */
public enum StreamPaymentType {

  PAYMENT_RECEIVED(BalanceAdjustmentType.CREDIT),
  PAYMENT_SENT(BalanceAdjustmentType.DEBIT);

  private BalanceAdjustmentType adjustmentType;

  StreamPaymentType(BalanceAdjustmentType adjustmentType) {
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
