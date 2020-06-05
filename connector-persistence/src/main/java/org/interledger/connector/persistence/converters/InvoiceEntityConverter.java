package org.interledger.connector.persistence.converters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.InvoiceId;

import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.util.Optional;

public class InvoiceEntityConverter implements Converter<InvoiceEntity, Invoice> {

  @Override
  public Invoice convert(InvoiceEntity invoiceEntity) {
    return Invoice.builder()
      .receiverInvoiceUrl(HttpUrl.get(invoiceEntity.getReceiverInvoiceUrl()))
      .id(InvoiceId.of(invoiceEntity.getInvoiceId()))
      .accountId(AccountId.of(invoiceEntity.getAccountId()))
      .amount(UnsignedLong.valueOf(invoiceEntity.getAmount()))
      .assetCode(invoiceEntity.getAssetCode())
      .assetScale((short) invoiceEntity.getAssetScale())
      .createdAt(Optional.ofNullable(invoiceEntity.getCreatedDate()).orElse(Instant.now()))
      .description(invoiceEntity.getDescription())
      .expiresAt(invoiceEntity.getExpiresAt())
      .finalizedAt(invoiceEntity.getFinalizedAt())
      .received(UnsignedLong.valueOf(invoiceEntity.getReceived()))
      .subject(invoiceEntity.getSubject())
      .updatedAt(Optional.ofNullable(invoiceEntity.getModifiedDate()).orElse(Instant.now()))
      .build();
  }
}
