package org.interledger.connector.persistence.converters;

import org.interledger.openpayments.CorrelationId;
import org.interledger.openpayments.Denomination;
import org.interledger.openpayments.InvoiceId;
import org.interledger.connector.persistence.entities.PaymentEntity;
import org.interledger.openpayments.Payment;
import org.interledger.openpayments.PaymentId;

import com.google.common.primitives.UnsignedLong;
import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.util.Optional;

public class PaymentEntityConverter implements Converter<PaymentEntity, Payment> {

  @Override
  public Payment convert(PaymentEntity paymentEntity) {
    return Payment.builder()
      .invoiceId(InvoiceId.of(paymentEntity.getInvoiceId()))
      .correlationId(CorrelationId.of(paymentEntity.getCorrelationId()))
      .paymentId(PaymentId.of(paymentEntity.getPaymentId()))
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
