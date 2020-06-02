package org.interledger.connector.persistence.converters;

import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.persistence.entities.InvoiceEntity;

import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.util.Optional;

public class InvoiceEntityConverter implements Converter<InvoiceEntity, Invoice> {

  @Override
  public Invoice convert(InvoiceEntity invoiceEntity) {
    return Invoice.builder()
      .account(HttpUrl.get(invoiceEntity.getAccountUrl()))
      .invoiceUrl(HttpUrl.get(invoiceEntity.getInvoiceUrl()))
      .originalInvoiceUrl(HttpUrl.get(invoiceEntity.getOriginalInvoiceUrl()))
      .subject(invoiceEntity.getSubject())
      .correlationId(CorrelationId.of(invoiceEntity.getCorrelationId()))
      .assetCode(invoiceEntity.getAssetCode())
      .assetScale((short) invoiceEntity.getAssetScale())
      .amount(UnsignedLong.valueOf(invoiceEntity.getAmount()))
      .expiresAt(invoiceEntity.getExpiresAt())
      .description(invoiceEntity.getDescription())
      .createdAt(Optional.ofNullable(invoiceEntity.getCreatedDate()).orElse(Instant.now()))
      .received(UnsignedLong.valueOf(invoiceEntity.getReceived()))
      .updatedAt(Optional.ofNullable(invoiceEntity.getModifiedDate()).orElse(Instant.now()))
      .build();
  }
}
