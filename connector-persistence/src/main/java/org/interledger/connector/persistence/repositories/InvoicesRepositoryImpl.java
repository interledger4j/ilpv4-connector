package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.persistence.entities.InvoiceEntity;

import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class InvoicesRepositoryImpl implements InvoicesRepositoryCustom {

  @Autowired
  @Lazy
  private ConversionService conversionService;

  @Autowired
  private InvoicesRepository invoicesRepository;

  @Override
  public Invoice saveInvoice(Invoice invoice) {
    Objects.requireNonNull(invoice);
    InvoiceEntity saved = invoicesRepository.save(new InvoiceEntity(invoice));
    return conversionService.convert(saved, Invoice.class);
  }

  @Override
  public Optional<Invoice> findInvoiceByInvoiceIdAndAccountId(InvoiceId invoiceId, AccountId accountId) {
    Objects.requireNonNull(invoiceId);
    Optional<InvoiceEntity> entity = invoicesRepository.findByInvoiceIdAndAccountId(invoiceId, accountId);
    return entity.map(e -> conversionService.convert(e, Invoice.class));
  }

  @Override
  public List<Invoice> findAllInvoicesByReceiverInvoiceUrl(HttpUrl receiverInvoiceUrl) {
    Objects.requireNonNull(receiverInvoiceUrl);
    return invoicesRepository.findAllByReceiverInvoiceUrl(receiverInvoiceUrl)
      .stream()
      .map(e -> conversionService.convert(e, Invoice.class))
      .collect(Collectors.toList());
  }

  @Override
  public List<Invoice> findAllInvoicesByCorrelationId(CorrelationId correlationId) {
    Objects.requireNonNull(correlationId);
    return invoicesRepository.findAllByCorrelationId(correlationId)
      .stream()
      .map(e -> conversionService.convert(e, Invoice.class))
      .collect(Collectors.toList());
  }
}
