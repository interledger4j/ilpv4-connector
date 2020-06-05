package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.openpayments.CorrelationId;
import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.InvoiceId;

import okhttp3.HttpUrl;

import java.util.List;
import java.util.Optional;

/**
 * Allows a {@link InvoicesRepository} to perform additional, custom logic not provided by Spring Data.
 *
 * @see "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.single-repository-behavior"
 */
public interface InvoicesRepositoryCustom {

  Invoice saveInvoice(Invoice invoice);

  Optional<Invoice> findInvoiceByInvoiceIdAndAccountId(InvoiceId invoiceId, AccountId accountId);

  List<Invoice> findAllInvoicesByReceiverInvoiceUrl(HttpUrl receiverInvoiceUrl);

  List<Invoice> findAllInvoicesByCorrelationId(CorrelationId correlationId);
}
