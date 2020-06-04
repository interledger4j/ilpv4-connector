package org.interleger.openpayments;


import org.interledger.connector.accounts.AccountId;
import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.InvoiceId;
import org.interledger.openpayments.NewInvoice;
import org.interledger.openpayments.PayInvoiceRequest;
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
   * @param accountId The {@link AccountId} that should be associated with the created {@link Invoice}.
   * @return The {@link Invoice} that was created.
   */
  Invoice createInvoice(final NewInvoice newInvoice, final AccountId accountId);

  /**
   * Get and save the latest state of the invoice owned by the receiver, either located at a remote OPS at
   * {@code receiverInvoiceUrl}, or on this OPS, if the invoice with that location does not already exist on this OPS.
   *
   * @param receiverInvoiceUrl The unique URL of the {@link Invoice}.
   * @param accountId The {@link AccountId} to associate the synced {@link Invoice} with.
   * @return The synced {@link Invoice}.
   * @throws InvoiceAlreadyExistsProblem if the {@link Invoice} has already been synced.
   */
  Invoice syncInvoice(final HttpUrl receiverInvoiceUrl, final AccountId accountId) throws InvoiceAlreadyExistsProblem;

  /**
   * Get an existing invoice.
   *
   * @param invoiceId The {@link InvoiceId} of the invoice to get.
   * @param accountId The {@link AccountId} associated with the {@link Invoice}.
   * @return The existing {@link Invoice} with the specified {@link InvoiceId} and {@link AccountId}.
   */
  Invoice getInvoice(final InvoiceId invoiceId, final AccountId accountId);

  /**
   * Update an existing {@link Invoice}.
   *
   * @param invoice An updated {@link Invoice}.
   * @param accountId The {@link AccountId} associated with the given {@link Invoice}.
   * @return The updated {@link Invoice}.
   */
  Invoice updateInvoice(final Invoice invoice, final AccountId accountId);

  /**
   * Generate {@link PaymentDetailsType} for any supported payment rail. Note that currently, Interledger payments
   * are the only payments that require this method.
   *
   * For ILP payments, this logic will largely be the same as an SPSP server's setup logic. However,
   * implementations should keep track of the invoice in the underlying payment layer.
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} this payment is being set up to pay.
   * @param accountId The {@link AccountId} associated with the {@link Invoice}.
   * @return The payment details necessary to pay an invoice.
   */
  PaymentDetailsType getPaymentDetails(final InvoiceId invoiceId, final AccountId accountId);

  /**
   * Make a payment towards an {@link Invoice}.
   *
   * Note that this method should only be implemented for custodial wallets which can make payments on behalf of a sender.
   * Non-custodial wallets should instead determine Payment Details outside of the OPS and execute the payment
   * from the client.
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} to pay.
   * @param senderAccountId The {@link AccountId} of the sender.
   * @param payInvoiceRequest Optional request body containing the amount to pay on the {@link Invoice}.
   * @return The result of the payment.
   */
  PaymentResultType payInvoice(
    final InvoiceId invoiceId,
    final AccountId senderAccountId,
    final Optional<PayInvoiceRequest> payInvoiceRequest
  );

}
