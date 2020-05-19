package org.interledger.connector.opa;


import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.PayInvoiceRequest;

import okhttp3.HttpUrl;

import java.util.Optional;

/**
 * Service layer interface for dealing with {@link Invoice}s in different Open Payments flows.
 */
public interface InvoiceService<PaymentResultType, PaymentDetailsType> {

  /**
   * Get an existing invoice.
   *
   * Implementations SHOULD get the latest received state of the {@link Invoice} from the underlying payment layer.
   *
   * @param invoiceId The {@link InvoiceId} of the invoice to get.
   * @return The existing {@link Invoice} with the specified {@link InvoiceId}
   */
  Invoice getInvoiceById(final InvoiceId invoiceId);

  /**
   * Get and save the latest state of the invoice located at a remote OPS at {@code invoiceUrl},
   * if the invoice with that location does not already exist on this Open Payments Server.
   *
   * @param invoiceUrl The unique URL of the {@link Invoice}.
   * @return The synced {@link Invoice}.
   */
  // TODO: Add exception to signature to indicate that this can only be called once per invoice.
  Invoice syncInvoice(final HttpUrl invoiceUrl);

  /**
   * Create a new invoice by storing it.
   *
   * @param invoice The {@link Invoice} to store.
   * @return The {@link Invoice} that was stored.
   */
  Invoice createInvoice(final Invoice invoice);

  /**
   * Update an existing {@link Invoice}.
   *
   * @param invoice An updated {@link Invoice}.
   * @return The updated {@link Invoice}.
   */
  Invoice updateInvoice(final Invoice invoice);

  /**
   * Generate {@link PaymentDetails} for any supported payment rail.
   *
   * For ILP payments, this logic will largely be the same as an SPSP server's setup logic. However,
   * implementations should keep track of the invoice in the underlying payment layer.
   *
   * For XRP payments, this will return an XRP address and the invoiceId encoded in Base64.
   *
   * The type of {@link PaymentDetails} returned is determined by the {@link Invoice#paymentNetwork()}.
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} this payment is being set up to pay.
   * @return The payment details necessary to pay an invoice.
   */
  // TODO: Always return ILP details
  PaymentDetailsType getPaymentDetails(final InvoiceId invoiceId);

  /**
   * Make a payment towards an {@link Invoice}.
   *
   * Note that this method should only be implemented for custodial wallets which can make payments on behalf of a sender.
   * Non-custodial wallets should instead get {@link PaymentDetails} for an {@link Invoice} and execute the payment
   * from the client.
   *
   * @param invoiceId The {@link InvoiceId} of the {@link Invoice} to pay.
   * @param senderAccountId The {@link AccountId} of the sender.
   * @param payInvoiceRequest Optional request body containing the amount to pay on the {@link Invoice}.
   * @return The result of the payment.
   */
  PaymentResultType payInvoice(final InvoiceId invoiceId, AccountId senderAccountId, Optional<PayInvoiceRequest> payInvoiceRequest);

}
