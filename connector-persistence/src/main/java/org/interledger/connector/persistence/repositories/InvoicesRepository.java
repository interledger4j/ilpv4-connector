package org.interledger.connector.persistence.repositories;

import org.interledger.connector.opay.InvoiceId;
import org.interledger.connector.persistence.entities.InvoiceEntity;

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
   * @return List of {@link InvoiceEntity}.
   */
  Optional<InvoiceEntity> findByInvoiceId(InvoiceId invoiceId);

}
