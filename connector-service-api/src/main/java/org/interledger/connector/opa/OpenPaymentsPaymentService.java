package org.interledger.connector.opa;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.PaymentDetails;

import com.google.common.primitives.UnsignedLong;

import java.util.concurrent.ExecutionException;

/**
 * Service interface for all payment specific Open Payments operations.
 *
 * @param <T> The type of payment response returned by a call to payInvoice.
 */
public interface OpenPaymentsPaymentService<T, K> {

  /**
   * Get the details necessary to make a payment for an invoice.
   *
   * For ILP payments, this will be an ILP address and shared secret.  For XRP payments, this will be an XRP address
   * and a hash of the invoice ID.
   *
   * @param invoice The subject of the invoice.
   * @return The {@link PaymentDetails} needed to pay an {@link Invoice}.
   */
  K getPaymentDetails(final Invoice invoice);

  /**
   * Pay the invoice with the specified {@link InvoiceId}, using {@link PaymentDetails} needed to make the payment.
   *
   * @param paymentDetails The {@link PaymentDetails} needed to make the payment.
   * @param senderAccountId The {@link AccountId} of the sender.
   * @param amount The amount to pay.
   * @param correlationId The {@link InvoiceId} of the {@link Invoice} to pay.
   * @return The result of the payment.
   * @throws ExecutionException
   * @throws InterruptedException
   */
  T payInvoice(
    final PaymentDetails paymentDetails,
    final AccountId senderAccountId,
    final UnsignedLong amount,
    final InvoiceId correlationId
  ) throws ExecutionException, InterruptedException;
}
