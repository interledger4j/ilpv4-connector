package org.interledger.connector.opa.persistence.repositories;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.persistence.entities.InvoiceEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;

import java.util.Objects;
import java.util.Optional;

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
  public Optional<Invoice> findInvoiceByInvoiceId(InvoiceId invoiceId) {
    Objects.requireNonNull(invoiceId);
    Optional<InvoiceEntity> entity = invoicesRepository.findByInvoiceId(invoiceId);
    return entity.map(e -> conversionService.convert(e, Invoice.class));
  }
}
