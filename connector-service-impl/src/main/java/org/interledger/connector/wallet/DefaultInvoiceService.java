package org.interledger.connector.wallet;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.problems.InvalidInvoiceSubjectProblem;
import org.interledger.connector.opa.model.problems.InvoiceNotFoundProblem;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class DefaultInvoiceService implements InvoiceService {

  private final InvoicesRepository invoicesRepository;
  private ConversionService conversionService;

  public DefaultInvoiceService(
    final InvoicesRepository invoicesRepository,
    ConversionService conversionService
  ) {
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
    Objects.requireNonNull(invoice);

    return invoicesRepository.findByInvoiceId(invoice.id())
      .map(entity -> {
        // Only allow update of amount received for now.
        entity.setReceived(invoice.received().longValue());
        invoicesRepository.save(entity);
        return entity;
      })
      .map(entity -> conversionService.convert(entity, Invoice.class))
      .orElseThrow(() -> new InvoiceNotFoundProblem(invoice.id()));
  }
}
