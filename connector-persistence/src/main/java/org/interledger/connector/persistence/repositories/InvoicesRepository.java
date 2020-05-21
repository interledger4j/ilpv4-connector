package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.InvoiceId;
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
   * @param accountId
   * @return the {@link InvoiceEntity} if present.
   */
  Optional<InvoiceEntity> findByInvoiceIdAndAccountId(String invoiceId, String accountId);

  /**
   * Find an {@link InvoiceEntity} by its natural identifier (i.e., the invoiceId as a String).
   *
   * @param invoiceId A {@link InvoiceId} corresponding to {@link InvoiceEntity#getInvoiceId()}.
   *
   * @param accountId
   * @return the {@link InvoiceEntity} if present.
   */
  default Optional<InvoiceEntity> findByInvoiceIdAndAccountId(InvoiceId invoiceId, AccountId accountId) {
    return findByInvoiceIdAndAccountId(invoiceId.value(), accountId.value());
  }

  Optional<InvoiceEntity> findByInvoiceUrlAndAccountId(String invoiceUrl, String accountId);

  default Optional<InvoiceEntity> findByInvoiceUrlAndAccountId(HttpUrl invoiceUrl, AccountId accountId) {
    return findByInvoiceUrlAndAccountId(invoiceUrl.toString(), accountId.value());
  }

  Optional<InvoiceEntity> findByInvoiceUrl(String invoiceUrl);

  default Optional<InvoiceEntity> findByInvoiceUrl(HttpUrl invoiceUrl) {
    return findByInvoiceUrl(invoiceUrl.toString());
  }

  Optional<InvoiceEntity> findByCorrelationIdAndAccountId(String correlationId, String accountId);

  default Optional<InvoiceEntity> findByCorrelationIdAndAccountId(CorrelationId correlationId, AccountId accountId) {
    return findByCorrelationIdAndAccountId(correlationId.value(), accountId.value());
  }

}
