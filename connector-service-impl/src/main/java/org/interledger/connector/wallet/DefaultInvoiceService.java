package org.interledger.connector.wallet;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.problems.InvoiceNotFoundProblem;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.connector.persistence.repositories.InvoicesRepository;

import org.springframework.core.convert.ConversionService;

import java.util.Objects;
import java.util.Optional;

public class DefaultInvoiceService implements InvoiceService {

  private final InvoicesRepository invoicesRepository;
  private final ConversionService conversionService;

  public DefaultInvoiceService(
    final InvoicesRepository invoicesRepository,
    ConversionService conversionService) {
    this.invoicesRepository = Objects.requireNonNull(invoicesRepository);
    this.conversionService = Objects.requireNonNull(conversionService);
  }

  @Override
  public Invoice getInvoiceById(InvoiceId invoiceId) {
    Objects.requireNonNull(invoiceId);

    return invoicesRepository.findInvoiceByInvoiceId(invoiceId)
      .orElseThrow(() -> new InvoiceNotFoundProblem(invoiceId));
  }

  @Override
  public Invoice createInvoice(Invoice invoice) {
    Objects.requireNonNull(invoice);

    return invoicesRepository.saveInvoice(invoice);
  }

  @Override
  public Invoice updateInvoice(Invoice invoice) {
    return invoicesRepository.findByInvoiceId(invoice.id())
      .map(entity -> {
        entity.setReceived(invoice.received().longValue());
        InvoiceEntity saved = invoicesRepository.save(entity);
        return saved;
      })
      .map(entity -> this.conversionService.convert(entity, Invoice.class))
      .orElseThrow(() -> new InvoiceNotFoundProblem(invoice.id()));
  }

  @Override
  public Optional<Invoice> onPayment(XrpPayment xrpPayment) {
    return Optional.empty();
  }

  @Override
  public Optional<Invoice> onPayment(StreamPayment streamPayment) {
    return Optional.empty();
  }
}
