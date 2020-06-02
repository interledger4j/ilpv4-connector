package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.persistence.entities.InvoiceEntity;

import okhttp3.HttpUrl;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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
   * @return the {@link InvoiceEntity} if present.
   */
  Optional<InvoiceEntity> findByInvoiceId(String invoiceId);

  /**
   * Find an {@link InvoiceEntity} by its natural identifier (i.e., the invoiceId as a String).
   *
   * @param invoiceId A {@link InvoiceId} corresponding to {@link InvoiceEntity#getInvoiceId()}.
   * @return the {@link InvoiceEntity} if present.
   */
  default Optional<InvoiceEntity> findByInvoiceId(InvoiceId invoiceId) {
    return findByInvoiceId(invoiceId.value());
  }

  Optional<InvoiceEntity> findByCorrelationIdAndAccountId(String correlationId, String accountId);

  default Optional<InvoiceEntity> findByCorrelationIdAndAccountId(CorrelationId correlationId, AccountId accountId) {
    return findByCorrelationIdAndAccountId(correlationId.value(), accountId.value());
  };

  List<InvoiceEntity> findAllByCorrelationId(String correlationId);

  default List<InvoiceEntity> findAllByCorrelationId(CorrelationId correlationId) {
    return findAllByCorrelationId(correlationId.value());
  }
}
