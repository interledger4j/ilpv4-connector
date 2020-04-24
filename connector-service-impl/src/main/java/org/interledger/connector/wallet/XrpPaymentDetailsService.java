package org.interledger.connector.wallet;

import org.interledger.connector.opa.PaymentDetailsService;

public class XrpPaymentDetailsService implements PaymentDetailsService {
  @Override
  public String getAddressFromInvoiceSubject(String subject) {
    return null;
  }
}
