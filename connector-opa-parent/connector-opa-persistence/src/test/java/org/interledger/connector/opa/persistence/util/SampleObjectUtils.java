package org.interledger.connector.opa.persistence.util;

import org.interledger.connector.opa.model.ImmutableInvoice;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;

import com.google.common.primitives.UnsignedLong;

import java.time.Instant;
import java.util.UUID;

public final class SampleObjectUtils {

  public static ImmutableInvoice createNewInvoice() {
    return Invoice.builder()
      .accountId("ricketycricket")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .description("A test invoice")
      .expiresAt(Instant.now().plusSeconds(60))
      .id(InvoiceId.of(UUID.randomUUID().toString()))
      .received(UnsignedLong.ZERO)
      .subject("PAY ME BRUH")
      .build();
  }
}
