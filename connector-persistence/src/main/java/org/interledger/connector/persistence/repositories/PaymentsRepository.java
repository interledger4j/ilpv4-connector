package org.interledger.connector.persistence.repositories;

import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentId;
import org.interledger.connector.persistence.entities.PaymentEntity;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Allows {@link Payment}s to be persisted to a datastore.
 */
@Repository
public interface PaymentsRepository extends CrudRepository<PaymentEntity, Long>, PaymentsRepositoryCustom {

  Optional<PaymentEntity> findByPaymentIdAndInvoicePrimaryKey(String paymentId, Long invoicePrimaryKey);

  default Optional<PaymentEntity> findByPaymentIdAndInvoicePrimaryKey(PaymentId paymentId, Long invoicePrimaryKey) {
    return findByPaymentIdAndInvoicePrimaryKey(paymentId.value(), invoicePrimaryKey);
  };
}
