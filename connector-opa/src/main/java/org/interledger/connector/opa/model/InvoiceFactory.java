package org.interledger.connector.opa.model;

import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;

import okhttp3.HttpUrl;

import java.util.function.Supplier;

public class InvoiceFactory {

  private final PaymentPointerResolver paymentPointerResolver;

  private final OpenPaymentsSettings openPaymentsSettings;

  public InvoiceFactory(PaymentPointerResolver paymentPointerResolver, Supplier<OpenPaymentsSettings> openPaymentsSettings) {
    this.paymentPointerResolver = paymentPointerResolver;
    this.openPaymentsSettings = openPaymentsSettings.get();
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

    HttpUrl invoiceUrl = identifierUrl
      .newBuilder()
      .addPathSegment("invoices")
      .addPathSegment(initialInvoice.id().value())
      .build();

    return Invoice.builder().from(initialInvoice)
      .invoiceUrl(invoiceUrl)
      .build();
  }
}
