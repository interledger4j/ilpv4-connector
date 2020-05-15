package org.interledger.connector.opa;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.PaymentDetails;

import com.google.common.primitives.UnsignedLong;

import java.util.concurrent.ExecutionException;

public interface OpenPaymentsPaymentService<T> {

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

  T payInvoice(
    final PaymentDetails paymentDetails,
    final AccountId senderAccountId,
    final UnsignedLong amount,
    final InvoiceId correlationId
  ) throws ExecutionException, InterruptedException;
}
