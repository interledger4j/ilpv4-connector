package org.interledger.connector.wallet;

import org.interledger.connector.opa.PaymentDetailsService;

import io.xpring.payid.PayIDClient;
import io.xpring.payid.PayIDException;

import java.util.Objects;

public class XrpPaymentDetailsService implements PaymentDetailsService {

  private PayIDClient payIDClient;

  public XrpPaymentDetailsService(PayIDClient payIDClient) {
    this.payIDClient = Objects.requireNonNull(payIDClient);
  }

  @Override
  public String getAddressFromInvoiceSubject(String subject) {
    try {
      return payIDClient.xrpAddressForPayID(subject);
    } catch (PayIDException e) {
      throw new RuntimeException(e);
    }
  }
}
