package org.interleger.openpayments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.openpayments.ApproveMandateRequest;
import org.interledger.openpayments.AuthorizationUrls;
import org.interledger.openpayments.CorrelationId;
import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.UserAuthorizationRequiredException;

import com.google.common.primitives.UnsignedLong;

import java.util.concurrent.ExecutionException;

/**
 * Service interface for all payment specific Open Payments operations.
 *
 * @param <PaymentResultType> The type of payment response returned by a call to payInvoice.
 * @param <PaymentDetailsType> The type of payment details returned by a call to getPaymentDetails
 */
// TODO: Move implementations of this to connector-server
  // TODO: Move this interface to openpayments-api
public interface PaymentSystemFacade<PaymentResultType, PaymentDetailsType> {

  /**
   * Get the details necessary to make a payment for an invoice.
   *
   * For ILP payments, this will be an ILP address and shared secret.  For XRP payments, this will be an XRP address
   * and a hash of the invoice ID.
   *
   * @param invoice The subject of the invoice.
   * @return The {@link PaymentDetailsType} needed to pay an {@link Invoice}.
   */
  PaymentDetailsType getPaymentDetails(final Invoice invoice);

  /**
   * Pay the invoice with the specified {@link CorrelationId}, using {@link PaymentDetailsType} needed to make the payment.
   *
   * @param paymentDetails The {@link PaymentDetailsType} needed to make the payment.
   * @param senderAccountId The {@link AccountId} of the sender.
   * @param amount The amount to pay.
   * @param correlationId The {@link CorrelationId} of the {@link Invoice} to pay.
   * @return The result of the payment.
   * @throws ExecutionException
   * @throws InterruptedException
   */
  PaymentResultType payInvoice(
    final PaymentDetailsType paymentDetails,
    final AccountId senderAccountId,
    final UnsignedLong amount,
    final CorrelationId correlationId
  ) throws ExecutionException, InterruptedException, UserAuthorizationRequiredException;

  Class<PaymentResultType> getResultType();
  Class<PaymentDetailsType> getDetailsType();

  AuthorizationUrls getMandateAuthorizationUrls(ApproveMandateRequest request);
}
