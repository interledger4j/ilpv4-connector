package org.interledger.connector.wallet;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PayId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;

import okhttp3.HttpUrl;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Factory class for creating {@link Invoice}s.
 */
public class InvoiceFactory {

  private final PaymentPointerResolver paymentPointerResolver;
  private final PayIdResolver payIdResolver;

  private final Supplier<OpenPaymentsSettings> openPaymentsSettings;

  private final Optional<String> opaUrlPath;

  public InvoiceFactory(
    PaymentPointerResolver paymentPointerResolver,
    PayIdResolver payIdResolver,
    Supplier<OpenPaymentsSettings> openPaymentsSettings,
    Optional<String> opaUrlPath
  ) {
    this.paymentPointerResolver = paymentPointerResolver;
    this.payIdResolver = payIdResolver;
    this.openPaymentsSettings = openPaymentsSettings;
    this.opaUrlPath = opaUrlPath;
  }

  /**
   * Populate the URL of an invoice.  This MUST be called before creating an {@link Invoice} in the database.
   *
   * @param initialInvoice An {@link Invoice} without a URL.
   * @return An {@link Invoice} with a URL.
   */
  public Invoice construct(Invoice initialInvoice) {
    if (initialInvoice.invoiceUrl().isPresent()) {
      return initialInvoice;
    }
//
//    HttpUrl identifierUrl;
//    try {
//      identifierUrl = paymentPointerResolver.resolveHttpUrl(PaymentPointer.of(initialInvoice.subject()));
//    } catch (IllegalArgumentException e) {
//      // Subject is a PayID
//      String payIdString = initialInvoice.subject();
//      if (!payIdString.startsWith("payid:")) {
//        payIdString = "payid:" + payIdString;
//      }
//      PayId payId = PayId.of(payIdString);
//      identifierUrl = payIdResolver.resolveHttpUrl(payId);
//    }
//
//    // largely for testing reasons since we may need to override and start on http instead of https
//    identifierUrl = identifierUrl.newBuilder().scheme(openPaymentsSettings.metadata().defaultScheme()).build();
    HttpUrl identifierUrl = openPaymentsSettings.get().metadata().issuer();

    // FIXME: Just tack on /invoices/{invoiceId} to the resolved payment identifier.  This requires our payment
    //  identifiers to start with /accounts
    // Only want the account part of the payment pointer path
    String paymentTarget = getAccount(initialInvoice.subject());

    HttpUrl invoiceUrl = new HttpUrl.Builder()
      .scheme(identifierUrl.scheme())
      .host(identifierUrl.host())
      .port(identifierUrl.port())
      .addPathSegment("accounts")
      .addPathSegment(paymentTarget)
      .addPathSegment("invoices")
      .addPathSegment(initialInvoice.id().value())
      .build();

    return Invoice.builder().from(initialInvoice)
      .invoiceUrl(invoiceUrl)
      .build();
  }

  public String getAccount(String subject) {
    try {
      if (subject.startsWith("payid:")) {
        return PayId.of(subject).account();
      } else {
        return PayId.of("payid:" + subject).account();
      }
    } catch (Exception e) {
      String paymentPointerPath = PaymentPointer.of(subject).path();
      if (paymentPointerPath.startsWith("/")) {
        paymentPointerPath = paymentPointerPath.substring(1);
      }
      return PaymentDetailsUtils.getPaymentTarget(paymentPointerPath, this.opaUrlPath);
    }
  }
}
