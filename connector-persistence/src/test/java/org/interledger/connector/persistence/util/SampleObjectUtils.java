package org.interledger.connector.persistence.util;

import org.interledger.connector.opa.model.ImmutableInvoice;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.PaymentNetwork;

import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;

import java.time.Instant;
import java.util.UUID;

public final class SampleObjectUtils {

  public static ImmutableInvoice createNewIlpInvoice() {
    return Invoice.builder()
      .accountId("ricketycricket")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .description("A test invoice")
      .expiresAt(Instant.now().plusSeconds(60))
      .id(InvoiceId.of(UUID.randomUUID().toString()))
      .received(UnsignedLong.ZERO)
      .subject("$xpring.money/paymebruh")
      .invoiceUrl(HttpUrl.get("https://xpring.money/paymebruh/invoices/1234"))
      .build();
  }

  public static ImmutableInvoice createNewXrpInvoice() {
    return Invoice.builder()
      .accountId("ricketycricket")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .description("A test invoice")
      .expiresAt(Instant.now().plusSeconds(60))
      .id(InvoiceId.of(UUID.randomUUID().toString()))
      .received(UnsignedLong.ZERO)
      .subject("paymebruh$xpring.money")
      .paymentNetwork(PaymentNetwork.XRPL)
      .build();
  }
}
