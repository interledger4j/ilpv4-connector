package org.interledger.connector.persistence.repositories;

import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentId;

import java.util.Optional;

/**
 * Allows a {@link PaymentsRepository} to perform additional, custom logic not provided by Spring Data.
 *
 * @see "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.single-repository-behavior"
 */
public interface PaymentsRepositoryCustom {

  Payment savePayment(Payment payment);

  Optional<Payment> findPaymentByPaymentIdAndInvoicePrimaryKey(PaymentId paymentId, Long invoicePrimaryKey);
}
