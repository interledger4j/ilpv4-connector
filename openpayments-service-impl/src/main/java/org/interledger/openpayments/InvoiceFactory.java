package org.interledger.openpayments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.openpayments.config.OpenPaymentsSettings;
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
   * Populate the URL of an invoice.  This MUST be called before creating an {@link Invoice} in the database.
   *
   * Note that this should only be called by the invoice owner's OPS.
   *
   * @param newInvoice An {@link Invoice} without a URL.
   * @return An {@link Invoice} with a URL.
   */
  public Invoice construct(NewInvoice newInvoice) {

    HttpUrl ownerAccountUrl = this.resolveInvoiceSubject(newInvoice.subject());

    String accountId = ownerAccountUrl.pathSegments().get(ownerAccountUrl.pathSegments().size() - 1);

    return Invoice.builder()
      .ownerAccountUrl(ownerAccountUrl)
      .accountId(AccountId.of(accountId))
      .amount(newInvoice.amount())
      .assetCode(newInvoice.assetCode())
      .assetScale(newInvoice.assetScale())
      .subject(newInvoice.subject())
      .description(newInvoice.description())
      .build();
  }

  public HttpUrl resolveInvoiceSubject(String subject) {
    HttpUrl ownerAccountUrl;
    try {
      ownerAccountUrl = paymentPointerResolver.resolveHttpUrl(PaymentPointer.of(subject));
    } catch (IllegalArgumentException e) {
      // Subject is a PayID
      if (!subject.startsWith("payid:")) {
        subject = "payid:" + subject;
      }
      PayId payId = PayId.of(subject);
      ownerAccountUrl = payIdResolver.resolveHttpUrl(payId);
    }

    // largely for testing reasons since we may need to override and start on http instead of https
    return ownerAccountUrl.newBuilder().scheme(openPaymentsSettings.metadata().defaultScheme()).build();
  }
}
