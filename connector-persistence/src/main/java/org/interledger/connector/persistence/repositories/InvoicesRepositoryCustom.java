package org.interledger.connector.persistence.repositories;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;

import java.util.Optional;

/**
 * Allows a {@link InvoicesRepository} to perform additional, custom logic not provided by Spring Data.
 *
 * @see "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.single-repository-behavior"
 */
public interface InvoicesRepositoryCustom {

  Invoice saveInvoice(Invoice invoice);

  Optional<Invoice> findInvoiceByInvoiceId(InvoiceId invoiceId);
}