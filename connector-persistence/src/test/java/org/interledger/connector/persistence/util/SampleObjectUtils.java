package org.interledger.connector.persistence.util;

import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.Denomination;
import org.interledger.connector.opa.model.ImmutableInvoice;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentId;

import com.google.common.primitives.UnsignedLong;

import java.time.Instant;
import java.util.UUID;

public final class SampleObjectUtils {

  public static ImmutableInvoice createNewIlpInvoice() {
    return Invoice.builder()
//      .accountId(AccountId.of("ricketycricket"))
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .description("A test invoice")
      .expiresAt(Instant.now().plusSeconds(60))
//      .id(InvoiceId.of(UUID.randomUUID().toString()))
      .received(UnsignedLong.ZERO)
      .subject("$xpring.money/paymebruh")
//      .invoiceUrl(HttpUrl.get("https://xpring.money/paymebruh/invoices/1234"))
      .build();
  }

  public static ImmutableInvoice createNewXrpInvoice() {
    return Invoice.builder()
//      .accountId(AccountId.of("ricketycricket"))
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .description("A test invoice")
      .expiresAt(Instant.now().plusSeconds(60))
//      .id(InvoiceId.of(UUID.randomUUID().toString()))
      .received(UnsignedLong.ZERO)
      .subject("paymebruh$xpring.money")
//      .invoiceUrl(HttpUrl.get("https://xpring.money/paymebruh/invoices/1234"))
      .build();
  }

  public static Payment createNewIlpPayment() {
    return Payment.builder()
      .correlationId(CorrelationId.of("f1546058d8e46d2903438bcf71e2af4381a4e5830e95787b66b299c0f30100de"))
      .paymentId(PaymentId.of(UUID.randomUUID().toString()))
      .sourceAddress("test.foo.bar")
      .destinationAddress("test.foo.baz.1234")
      .amount(UnsignedLong.ONE)
      .denomination(Denomination.builder()
        .assetCode("XRP")
        .assetScale((short) 6)
        .build())
      .build();
  }

  public static Payment createNewXrpPayment() {
    return Payment.builder()
      .correlationId(CorrelationId.of("f1546058d8e46d2903438bcf71e2af4381a4e5830e95787b66b299c0f30100de"))
      .paymentId(PaymentId.of(UUID.randomUUID().toString()))
      .sourceAddress("rPm88mdDuXLgxzpmZPXf6wPQ1ZTHRNvYVr 123456")
      .destinationAddress("rDJFnv5sEfp42LMFiX3mVQKczpFTdxYDzM 123456")
      .amount(UnsignedLong.ONE)
      .denomination(Denomination.builder()
        .assetCode("XRP")
        .assetScale((short) 6)
        .build())
      .build();
  }
}
