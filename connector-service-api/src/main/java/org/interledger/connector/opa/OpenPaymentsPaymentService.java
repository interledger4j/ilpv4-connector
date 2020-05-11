package org.interledger.connector.opa;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.PaymentDetails;

public interface OpenPaymentsPaymentService {

  /**
   * Get the details necessary to make a payment for an invoice.
   *
   * For ILP payments, this will be an ILP address and shared secret.  For XRP payments, this will be an XRP address
   * and a hash of the invoice ID.
   *
   * @param invoice The subject of the invoice.
   * @return The {@link PaymentDetails} needed to pay an {@link Invoice}.
   */
  PaymentDetails getPaymentDetails(final Invoice invoice);

}
