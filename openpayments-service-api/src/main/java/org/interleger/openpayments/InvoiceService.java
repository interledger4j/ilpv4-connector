package org.interleger.openpayments;


import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.InvoiceId;
import org.interledger.openpayments.NewInvoice;
import org.interledger.openpayments.PayIdAccountId;
import org.interledger.openpayments.PayInvoiceRequest;
import org.interledger.openpayments.UserAuthorizationRequiredException;
import org.interledger.openpayments.problems.InvoiceAlreadyExistsProblem;

import okhttp3.HttpUrl;

import java.util.Optional;

/**
 * Service layer interface for dealing with {@link Invoice}s in different Open Payments flows.
 */
public interface InvoiceService<PaymentResultType, PaymentDetailsType> {

  /**
   * Create a new invoice at the invoices URL resolved from {@code invoice#subject()}.
   *
   * @param newInvoice The {@link Invoice} to create.
   * @param payIdAccountId The {@link PayIdAccountId} that should be associated with the created {@link Invoice}.
   * @return The {@link Invoice} that was created.
   */
  Invoice createInvoice(final NewInvoice newInvoice, final PayIdAccountId payIdAccountId);

  Optional<Invoice> findInvoiceByUrl(HttpUrl invoiceUrl, PayIdAccountId payIdAccountId);

  /**
   * Get and save the latest state of the invoice owned by the receiver, either located at a remote OPS at
   * {@code receiverInvoiceUrl}, or on this OPS, if the invoice with that location does not already exist on this OPS.
   *
   * @param receiverInvoiceUrl The unique URL of the {@link Invoice}.
   * @param payIdAccountId The {@link PayIdAccountId} to associate the synced {@link Invoice} with.
   * @return The synced {@link Invoice}.
   * @throws InvoiceAlreadyExistsProblem if the {@link Invoice} has already been synced.
   */
  Invoice syncInvoice(final HttpUrl receiverInvoiceUrl, final PayIdAccountId payIdAccountId) throws InvoiceAlreadyExistsProblem;

  /**
   * Get an existing invoice.
   *
   * @param invoiceId The {@link InvoiceId} of the invoice to get.
   * @param payIdAccountId
   * @return The existing {@link Invoice} with the specified {@link InvoiceId} and {@link PayIdAccountId}.
   */
  Invoice getInvoice(final InvoiceId invoiceId, PayIdAccountId payIdAccountId);

  /**
   * Update an existing {@link Invoice}.
   *
   * @param invoice An updated {@link Invoice}.
   * @param payIdAccountId The {@link PayIdAccountId} associated with the given {@link Invoice}.
   * @return The updated {@link Invoice}.
   */
  Invoice updateInvoice(final Invoice invoice, final PayIdAccountId payIdAccountId);

  /**
   * Generate {@link PaymentDetailsType} for any supported payment rail. Note that currently, Interledger payments
   * are the only payments that require this method.
   *
   * For ILP payments, this logic will largely be the same as an SPSP server's setup logic. However,
   * implementations should keep track of the invoice in the underlying payment layer.
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} this payment is being set up to pay.
   * @param payIdAccountId The {@link PayIdAccountId} associated with the {@link Invoice}.
   * @return The payment details necessary to pay an invoice.
   */
  PaymentDetailsType getPaymentDetails(final InvoiceId invoiceId, final PayIdAccountId payIdAccountId);

  /**
   * Make a payment towards an {@link Invoice}.
   *
   * Note that this method should only be implemented for custodial wallets which can make payments on behalf of a sender.
   * Non-custodial wallets should instead determine Payment Details outside of the OPS and execute the payment
   * from the client.
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} to pay.
   * @param senderPayIdAccountId The {@link PayIdAccountId} of the sender.
   * @param payInvoiceRequest Optional request body containing the amount to pay on the {@link Invoice}.
   * @return The result of the payment.
   */
  PaymentResultType payInvoice(
    final InvoiceId invoiceId,
    final PayIdAccountId senderPayIdAccountId,
    final Optional<PayInvoiceRequest> payInvoiceRequest
  ) throws UserAuthorizationRequiredException;

  Class<PaymentResultType> getResultType();

  Class<PaymentDetailsType> getRequestType();
}
