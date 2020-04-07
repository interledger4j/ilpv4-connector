package org.interledger.connector.opa.persistence.converters;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.persistence.entities.InvoiceEntity;

import com.google.common.primitives.UnsignedLong;
import org.springframework.core.convert.converter.Converter;

import java.util.UUID;

public class InvoiceEntityConverter implements Converter<InvoiceEntity, Invoice> {

  @Override
  public Invoice convert(InvoiceEntity invoiceEntity) {
    return Invoice.builder()
      .accountId(invoiceEntity.getAccountId())
      .amount(UnsignedLong.valueOf(invoiceEntity.getAmount()))
      .assetCode(invoiceEntity.getAssetCode())
      .assetScale((short) invoiceEntity.getAssetScale())
      .createdAt(invoiceEntity.getCreatedDate())
      .description(invoiceEntity.getDescription())
      .expiresAt(invoiceEntity.getExpiresAt())
      .finalizedAt(null) // fixme
      .id(InvoiceId.of(UUID.fromString(invoiceEntity.getInvoiceId())))
      .received(UnsignedLong.valueOf(invoiceEntity.getReceived()))
      .subject(invoiceEntity.getSubject())
      .updatedAt(invoiceEntity.getModifiedDate())
      .build();
  }
}
