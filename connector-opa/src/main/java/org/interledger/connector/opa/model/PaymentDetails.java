package org.interledger.connector.opa.model;

import org.immutables.value.Value;

/**
 * Parent interface of payment details for any payment rail.
 *
 * This parent interface is used solely to provide clean inheritance among different payment detail types,
 * though sub-interfaces of {@link PaymentDetails} may have any number of fields.
 */
@Value.Immutable
public interface PaymentDetails {

  static ImmutablePaymentDetails.Builder builder() {
    return ImmutablePaymentDetails.builder();
  }
}
