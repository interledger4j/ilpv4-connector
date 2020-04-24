package org.interledger.connector.opa;

public interface PaymentDetailsService {

  /**
   * Get the payment address of an invoice subject in order to pay an invoice.
   *
   * For ILP payments, this will be an ILP address.  For XRP payments, this will be an XRP address.
   *
   * @param subject The subject of the invoice.
   * @return The address at which a sender can send money to pay off an invoice.
   */
  String getAddressFromInvoiceSubject(final String subject);

}
