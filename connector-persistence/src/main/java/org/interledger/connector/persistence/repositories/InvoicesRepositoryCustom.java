package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;

import okhttp3.HttpUrl;

import java.util.Optional;

/**
 * Allows a {@link InvoicesRepository} to perform additional, custom logic not provided by Spring Data.
 *
 * @see "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.single-repository-behavior"
 */
public interface InvoicesRepositoryCustom {

  Invoice saveInvoice(Invoice invoice);

  Optional<Invoice> findInvoiceByInvoiceIdAndAccountId(InvoiceId invoiceId, AccountId accountId);

  Optional<Invoice> findInvoiceByInvoiceUrlAndAccountId(HttpUrl invoiceUrl, AccountId accountId);

  Optional<Invoice> findInvoiceByInvoiceUrl(HttpUrl invoiceUrl);

  Optional<Invoice> findInvoiceByCorrelationIdAndAccountId(CorrelationId correlationId, AccountId accountId);
}
