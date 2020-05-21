package org.interledger.connector.persistence.repositories;

import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentId;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.connector.persistence.entities.PaymentEntity;

import okhttp3.HttpUrl;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Allows {@link Payment}s to be persisted to a datastore.
 */
@Repository
public interface PaymentsRepository extends CrudRepository<PaymentEntity, Long>, PaymentsRepositoryCustom {

  /**
   * Find an {@link PaymentEntity} by its natural identifier (i.e., the paymentId as a String).
   *
   * @param paymentId A {@link String} corresponding to {@link PaymentEntity#getPaymentId()}.
   *
   * @return the {@link PaymentEntity} if present.
   */
  Optional<PaymentEntity> findByPaymentId(String paymentId);

  /**
   * Find an {@link PaymentEntity} by its natural identifier (i.e., the invoiceId as a String).
   *
   * @param paymentId A {@link InvoiceId} corresponding to {@link PaymentEntity#getPaymentId()}.
   *
   * @return the {@link PaymentEntity} if present.
   */
  default Optional<PaymentEntity> findByPaymentId(PaymentId paymentId) {
    return findByPaymentId(paymentId.value());
  }

  /**
   * Find an {@link PaymentEntity} by its correlationId.
   *
   * @param correlationId A {@link String} corresponding to {@link PaymentEntity#getCorrelationId()}.
   *
   * @return the {@link PaymentEntity} if present.
   */
  Optional<PaymentEntity> findByCorrelationId(String correlationId);
}
