package org.interledger.connector.wallet;

import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.opa.model.CreateInvoiceRequest;
import org.interledger.connector.opa.model.ImmutableInvoice;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PayId;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;

import okhttp3.HttpUrl;

import java.util.function.Supplier;

/**
 * Factory class for creating {@link Invoice}s.
 */
public class InvoiceFactory {

  private final PaymentPointerResolver paymentPointerResolver;
  private final PayIdResolver payIdResolver;

  private final OpenPaymentsSettings openPaymentsSettings;

  public InvoiceFactory(
    PaymentPointerResolver paymentPointerResolver,
    PayIdResolver payIdResolver,
    Supplier<OpenPaymentsSettings> openPaymentsSettings
  ) {
    this.paymentPointerResolver = paymentPointerResolver;
    this.payIdResolver = payIdResolver;
    this.openPaymentsSettings = openPaymentsSettings.get();
  }

  /**
   * Populate the account URL of an invoice, which will populate the invoiceUrl and accountId of an invoice.
   * This MUST be called before creating an {@link Invoice} in the database.
   *
   * @param createInvoiceRequest An {@link Invoice} without a URL.
   * @return An {@link Invoice} with a URL.
   */
  public Invoice construct(CreateInvoiceRequest createInvoiceRequest) {
    HttpUrl identifierUrl;
    try {
      identifierUrl = paymentPointerResolver.resolveHttpUrl(PaymentPointer.of(createInvoiceRequest.subject()));
    } catch (IllegalArgumentException e) {
      // Subject is a PayID
      String payIdString = createInvoiceRequest.subject();
      if (!payIdString.startsWith("payid:")) {
        payIdString = "payid:" + payIdString;
      }
      PayId payId = PayId.of(payIdString);
      identifierUrl = payIdResolver.resolveHttpUrl(payId);
    }

    // largely for testing reasons since we may need to override and start on http instead of https
    identifierUrl = identifierUrl.newBuilder().scheme(openPaymentsSettings.metadata().defaultScheme()).build();

    return Invoice.builder()
      .account(identifierUrl)
      .subject(createInvoiceRequest.subject())
      .assetCode(createInvoiceRequest.assetCode())
      .assetScale(createInvoiceRequest.assetScale())
      .assetScale((short) 9)
      .amount(createInvoiceRequest.amount())
      .build();
  }
}
