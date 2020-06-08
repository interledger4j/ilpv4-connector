package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.openpayments.CorrelationId;
import org.interledger.openpayments.InvoiceId;

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

  List<InvoiceEntity> findAllByReceiverInvoiceUrl(String receiverInvoiceUrl);

  default List<InvoiceEntity> findAllByReceiverInvoiceUrl(HttpUrl receiverInvoiceUrl) {
    return findAllByReceiverInvoiceUrl(receiverInvoiceUrl.toString());
  };

  List<InvoiceEntity> findAllByCorrelationId(String correlationId);

  default List<InvoiceEntity> findAllByCorrelationId(CorrelationId correlationId) {
    return findAllByCorrelationId(correlationId.value());
  }
}
