package org.interledger.connector.wallet;

import org.interledger.connector.opa.PaymentDetailsService;

public class PayIdPaymentDetailsService implements PaymentDetailsService {
  @Override
  public String getAddressFromInvoiceSubject(String subject) {
    return null;
  }
}
