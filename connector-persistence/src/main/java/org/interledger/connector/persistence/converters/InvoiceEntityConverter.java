package org.interledger.connector.persistence.converters;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.PaymentNetwork;
import org.interledger.connector.persistence.entities.InvoiceEntity;

import com.google.common.primitives.UnsignedLong;
import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.util.Optional;

public class InvoiceEntityConverter implements Converter<InvoiceEntity, Invoice> {

  @Override
  public Invoice convert(InvoiceEntity invoiceEntity) {
    return Invoice.builder()
      .accountId(invoiceEntity.getAccountId())
      .amount(UnsignedLong.valueOf(invoiceEntity.getAmount()))
      .assetCode(invoiceEntity.getAssetCode())
      .assetScale((short) invoiceEntity.getAssetScale())
      .createdAt(Optional.ofNullable(invoiceEntity.getCreatedDate()).orElse(Instant.now()))
      .description(invoiceEntity.getDescription())
      .expiresAt(invoiceEntity.getExpiresAt())
      .finalizedAt(invoiceEntity.getFinalizedAt())
      .id(InvoiceId.of(invoiceEntity.getInvoiceId()))
      .received(UnsignedLong.valueOf(invoiceEntity.getReceived()))
      .subject(invoiceEntity.getSubject())
      .updatedAt(Optional.ofNullable(invoiceEntity.getModifiedDate()).orElse(Instant.now()))
      .paymentNetwork(PaymentNetwork.valueOf(invoiceEntity.getPaymentNetwork()))
      .build();
  }
}
