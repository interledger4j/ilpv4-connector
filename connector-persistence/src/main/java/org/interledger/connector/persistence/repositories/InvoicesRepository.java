package org.interledger.connector.persistence.repositories;

import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.PaymentId;
import org.interledger.connector.persistence.entities.InvoiceEntity;

import okhttp3.HttpUrl;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Allows Invoices to be persisted to a datastore.
 */
@Repository
public interface InvoicesRepository extends CrudRepository<InvoiceEntity, Long>, InvoicesRepositoryCustom {

  /**
   * Find an {@link InvoiceEntity} by its natural identifier (i.e., the invoiceId as a String).
   *
   * @param invoiceId A {@link String} corresponding to {@link InvoiceEntity#getInvoiceId()}.
   *
   * @return the {@link InvoiceEntity} if present.
   */
  Optional<InvoiceEntity> findByInvoiceId(String invoiceId);

  /**
   * Find an {@link InvoiceEntity} by its natural identifier (i.e., the invoiceId as a String).
   *
   * @param invoiceId A {@link InvoiceId} corresponding to {@link InvoiceEntity#getInvoiceId()}.
   *
   * @return the {@link InvoiceEntity} if present.
   */
  default Optional<InvoiceEntity> findByInvoiceId(InvoiceId invoiceId) {
    return findByInvoiceId(invoiceId.value());
  }

  Optional<InvoiceEntity> findByInvoiceUrl(String invoiceUrl);

  default Optional<InvoiceEntity> findByInvoiceUrl(HttpUrl invoiceUrl) {
    return findByInvoiceUrl(invoiceUrl.toString());
  }

  Optional<InvoiceEntity> findByPaymentId(String paymentId);

  default Optional<InvoiceEntity> findByPaymentId(PaymentId paymentId) {
    return findByPaymentId(paymentId.value());
  }

}
