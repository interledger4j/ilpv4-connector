package org.interledger.connector.wallet;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;

import okhttp3.HttpUrl;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class InvoiceFactory {

  private final PaymentPointerResolver paymentPointerResolver;

  private final OpenPaymentsSettings openPaymentsSettings;

  private final Optional<String> opaUrlPath;

  public InvoiceFactory(
    PaymentPointerResolver paymentPointerResolver,
    Supplier<OpenPaymentsSettings> openPaymentsSettings,
    Optional<String> opaUrlPath
  ) {
    this.paymentPointerResolver = paymentPointerResolver;
    this.openPaymentsSettings = openPaymentsSettings.get();
    this.opaUrlPath = opaUrlPath;
  }

  public Invoice construct(Invoice initialInvoice) {
    if (initialInvoice.invoiceUrl().isPresent()) {
      return initialInvoice;
    }

    HttpUrl identifierUrl;
    try {
      identifierUrl = paymentPointerResolver.resolveHttpUrl(PaymentPointer.of(initialInvoice.subject()));
      // largely for testing reasons since we may need to override and start on http instead of https
      identifierUrl = identifierUrl.newBuilder().scheme(openPaymentsSettings.metadata().defaultScheme()).build();
    } catch (IllegalArgumentException e) {
      // Subject is a PayID
      String[] payIdParts = initialInvoice.subject().split("$");
      identifierUrl = new HttpUrl.Builder()
        .scheme(openPaymentsSettings.metadata().defaultScheme())
        .host(payIdParts[1])
        .addPathSegment(payIdParts[0])
        .build();
    }

    // Only want the account part of the payment pointer path
    String paymentPointerPath = identifierUrl.pathSegments().stream().reduce("", (s, s2) -> s + "/" + s2);
    if (paymentPointerPath.startsWith("/")) {
      paymentPointerPath = paymentPointerPath.substring(1);
    }
    String paymentTarget = PaymentDetailsUtils.getPaymentTarget(paymentPointerPath, this.opaUrlPath);

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
}
