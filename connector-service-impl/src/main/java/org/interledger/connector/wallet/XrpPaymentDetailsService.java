package org.interledger.connector.wallet;

import org.interledger.connector.opa.PaymentDetailsService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.PaymentDetails;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.connector.opa.model.problems.InvoicePaymentDetailsProblem;

import io.xpring.payid.PayIDClient;
import io.xpring.payid.PayIDException;

import java.util.Objects;

public class XrpPaymentDetailsService implements PaymentDetailsService {

  private PayIDClient payIDClient;

  public XrpPaymentDetailsService(PayIDClient payIDClient) {
    this.payIDClient = Objects.requireNonNull(payIDClient);
  }

  @Override
  public PaymentDetails getPaymentDetails(Invoice invoice) {
    try {
      String xrpAddress = payIDClient.xrpAddressForPayID(invoice.subject());
      return XrpPaymentDetails.builder()
        .address(xrpAddress)
        .invoiceIdHash(invoice.paymentId())
        .build();
    } catch (PayIDException e) {
      throw new InvoicePaymentDetailsProblem("Could not get destination address for given invoice subject.", invoice.id());
    }
  }
}
