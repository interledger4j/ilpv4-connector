package org.interledger.connector.persistence.repositories;

import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentId;
import org.interledger.connector.persistence.entities.PaymentEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;

import java.util.Objects;
import java.util.Optional;

public class PaymentsRepositoryImpl implements PaymentsRepositoryCustom {

  @Autowired
  @Lazy
  private ConversionService conversionService;

  @Autowired
  private PaymentsRepository paymentsRepository;

  @Override
  public Payment savePayment(Payment payment) {
    Objects.requireNonNull(payment);
    PaymentEntity saved = paymentsRepository.save(new PaymentEntity(payment));
    return conversionService.convert(saved, Payment.class);
  }

  @Override
  public Optional<Payment> findPaymentByPaymentIdAndInvoiceId(PaymentId paymentId, InvoiceId invoiceId) {
    Objects.requireNonNull(paymentId);
    Objects.requireNonNull(invoiceId);
    return paymentsRepository.findByPaymentIdAndInvoiceId(paymentId, invoiceId)
      .map(e -> conversionService.convert(e, Payment.class));
  }
}
