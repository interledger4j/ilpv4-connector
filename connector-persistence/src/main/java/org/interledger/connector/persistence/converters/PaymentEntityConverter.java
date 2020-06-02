package org.interledger.connector.persistence.converters;

import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.Denomination;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentId;
import org.interledger.connector.persistence.entities.PaymentEntity;

import com.google.common.primitives.UnsignedLong;
import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.util.Optional;

public class PaymentEntityConverter implements Converter<PaymentEntity, Payment> {

  @Override
  public Payment convert(PaymentEntity paymentEntity) {
    return Payment.builder()
      .paymentId(PaymentId.of(paymentEntity.getPaymentId()))
      .invoiceId(InvoiceId.of(paymentEntity.getInvoiceId()))
      .correlationId(CorrelationId.of(paymentEntity.getCorrelationId()))
      .sourceAddress(paymentEntity.getSourceAddress())
      .destinationAddress(paymentEntity.getDestinationAddress())
      .amount(UnsignedLong.valueOf(paymentEntity.getAmount()))
      .denomination(Denomination.builder()
        .assetCode(paymentEntity.getAssetCode())
        .assetScale((short) paymentEntity.getAssetScale())
        .build())
      .createdAt(Optional.ofNullable(paymentEntity.getCreatedDate()).orElse(Instant.now()))
      .modifiedAt(Optional.ofNullable(paymentEntity.getModifiedDate()).orElse(Instant.now()))
      .build();
  }
}
