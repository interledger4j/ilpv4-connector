package org.interledger.connector.wallet;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.OpenPaymentsPaymentService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.PaymentDetails;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.connector.opa.model.problems.InvoicePaymentDetailsProblem;
import org.interledger.stream.SendMoneyResult;

import com.google.common.primitives.UnsignedLong;
import io.xpring.payid.PayIDClient;
import io.xpring.payid.PayIDException;

import java.util.Objects;

public class XrpOpenPaymentsPaymentService implements OpenPaymentsPaymentService<SendMoneyResult> {

  private PayIDClient payIDClient;

  public XrpOpenPaymentsPaymentService(PayIDClient payIDClient) {
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

  @Override
  public SendMoneyResult payInvoice(PaymentDetails paymentDetails, AccountId senderAccountId, UnsignedLong amount, InvoiceId invoiceId) {
    // TODO: Throw an exception here because we can't send XRP from OPS
    return null;
  }
}
