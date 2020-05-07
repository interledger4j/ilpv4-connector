package org.interledger.connector.opa.model;

import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;

import okhttp3.HttpUrl;

public class InvoiceFactory {

  private PaymentPointerResolver paymentPointerResolver;

  public InvoiceFactory(PaymentPointerResolver paymentPointerResolver) {
    this.paymentPointerResolver = paymentPointerResolver;
  }

  public Invoice construct(Invoice initialInvoice) {
    if (initialInvoice.invoiceUrl().isPresent()) {
      return initialInvoice;
    }

    HttpUrl identifierUrl;
    try {
      identifierUrl = paymentPointerResolver.resolveHttpUrl(PaymentPointer.of(initialInvoice.subject()));
    } catch (IllegalArgumentException e) {
      // Subject is a PayID
      String[] payIdParts = initialInvoice.subject().split("$");
      identifierUrl = new HttpUrl.Builder()
        .scheme("https")
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
