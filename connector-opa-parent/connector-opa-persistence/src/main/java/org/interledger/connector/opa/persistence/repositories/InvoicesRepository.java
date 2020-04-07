package org.interledger.connector.opa.persistence.repositories;


import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.persistence.entities.InvoiceEntity;

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

}
