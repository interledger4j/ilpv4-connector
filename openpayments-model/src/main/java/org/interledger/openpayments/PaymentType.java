package org.interledger.openpayments;

import org.interledger.connector.opa.model.Payment;

/**
 * Types of {@link Payment} that the system tracks.
 */
public enum PaymentType {
  DEBIT,
  CREDIT
}
