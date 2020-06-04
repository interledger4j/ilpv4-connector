package org.interledger.connector.persistence.repositories;

import org.interledger.openpayments.InvoiceId;
import org.interledger.openpayments.Payment;
import org.interledger.connector.persistence.entities.PaymentEntity;
import org.interledger.openpayments.PaymentId;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Allows {@link Payment}s to be persisted to a datastore.
 */
@Repository
public interface PaymentsRepository extends CrudRepository<PaymentEntity, Long>, PaymentsRepositoryCustom {

  Optional<PaymentEntity> findByPaymentIdAndInvoiceId(String paymentId, String invoiceId);

  default Optional<PaymentEntity> findByPaymentIdAndInvoiceId(PaymentId paymentId, InvoiceId invoiceId) {
    return findByPaymentIdAndInvoiceId(paymentId.value(), invoiceId.value());
  };
}
