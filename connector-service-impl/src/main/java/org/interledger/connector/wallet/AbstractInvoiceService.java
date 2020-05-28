package org.interledger.connector.wallet;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PayInvoiceRequest;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.problems.InvoiceAlreadyExistsProblem;
import org.interledger.connector.opa.model.problems.InvoiceNotFoundProblem;
import org.interledger.connector.opa.model.problems.UnsupportedInvoiceOperationProblem;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.persistence.repositories.PaymentsRepository;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import feign.FeignException;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractInvoiceService<PaymentResultType, PaymentDetailsType> implements InvoiceService<PaymentResultType, PaymentDetailsType> {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected final InvoicesRepository invoicesRepository;
  protected final PaymentsRepository paymentsRepository;
  protected final ConversionService conversionService;
  protected final InvoiceFactory invoiceFactory;
  protected final OpenPaymentsProxyClient openPaymentsProxyClient;
  protected final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;

  public AbstractInvoiceService(
    final InvoicesRepository invoicesRepository,
    PaymentsRepository paymentsRepository, ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsProxyClient openPaymentsProxyClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    EventBus eventBus
  ) {
    this.invoicesRepository = Objects.requireNonNull(invoicesRepository);
    this.paymentsRepository = Objects.requireNonNull(paymentsRepository);
    this.conversionService = Objects.requireNonNull(conversionService);
    this.invoiceFactory = Objects.requireNonNull(invoiceFactory);
    this.openPaymentsProxyClient = Objects.requireNonNull(openPaymentsProxyClient);
    this.openPaymentsSettingsSupplier = Objects.requireNonNull(openPaymentsSettingsSupplier);
    eventBus.register(this);
  }

  @Override
  public Invoice getInvoiceById(InvoiceId invoiceId, AccountId accountId) {
    Objects.requireNonNull(invoiceId);

    return invoicesRepository.findInvoiceByInvoiceIdAndAccountId(invoiceId, accountId)
      .orElseThrow(() -> new InvoiceNotFoundProblem(invoiceId));
  }

  @Override
  public Invoice syncInvoice(HttpUrl invoiceUrl, AccountId accountId) {
    Objects.requireNonNull(invoiceUrl);

    // See if we have that invoice already
    Optional<Invoice> existingInvoice = invoicesRepository.findInvoiceByInvoiceUrlAndAccountId(invoiceUrl, accountId);
    if (existingInvoice.isPresent()) {
      throw new InvoiceAlreadyExistsProblem(existingInvoice.get().id());
    } else {
      // If not, get it from the receiver, or just copy it locally
      Invoice invoiceOfReceiver;
      if (!isForThisWallet(invoiceUrl)) {
        invoiceOfReceiver = this.getRemoteInvoice(invoiceUrl);
      } else {
        invoiceOfReceiver = invoicesRepository.findAllInvoicesByInvoiceUrl(invoiceUrl)
          .stream()
          .filter(invoice -> {
            return invoice.subject().contains(invoice.accountId()); // FIXME: What's the best way to figure out if an invoice is owned by the receiver?
          })
          .findFirst()
          .orElseThrow(() -> new InvoiceNotFoundProblem(invoiceUrl));
      }

      // And set the accountId of the invoice to the sender's accountId
      Invoice invoiceWithCorrectAccountId = Invoice.builder()
        .from(invoiceOfReceiver)
        .accountId(accountId.value())
        .build();
      Invoice invoice = invoicesRepository.saveInvoice(invoiceWithCorrectAccountId);
      return invoice;
    }
  }

  private Invoice getRemoteInvoice(HttpUrl invoiceUrl) {
    try {
      return openPaymentsProxyClient.getInvoice(invoiceUrl.uri());
    } catch (FeignException e) {
      if (e.status() == 404) {
        throw new InvoiceNotFoundProblem("Original invoice was not found in invoice owner's records.", invoiceUrl);
      }

      throw e;
    }
  }

  @Override
  public Invoice createInvoice(Invoice invoice, AccountId accountId) {
    Objects.requireNonNull(invoice);

    Invoice invoiceWithUrl = invoiceFactory.construct(invoice);
    // This creation was meant for another...

    HttpUrl invoiceUrl = invoiceWithUrl.invoiceUrl().get();

    Invoice invoiceToSave = invoiceWithUrl;
    if (!isForThisWallet(invoiceUrl)) {
      HttpUrl createUrl = new HttpUrl.Builder()
        .scheme(invoiceUrl.scheme())
        .host(invoiceUrl.host())
        .port(invoiceUrl.port())
        .addPathSegment("accounts")
        .addPathSegment(invoice.accountId())
        .addPathSegment("invoices")
        .build();
      invoiceToSave = openPaymentsProxyClient.createInvoice(createUrl.uri(), invoice);
    }

    Invoice invoiceWithCorrectAccountId = Invoice.builder()
      .from(invoiceToSave)
      .accountId(accountId.value())
      .build();
    return invoicesRepository.saveInvoice(invoiceWithCorrectAccountId);
  }

  @Override
  public Invoice updateInvoice(Invoice invoice, AccountId accountId) {
    return invoicesRepository.findByInvoiceIdAndAccountId(invoice.id(), accountId)
      .map(entity -> {
        entity.setReceived(invoice.received().longValue());
        InvoiceEntity saved = invoicesRepository.save(entity);
        return saved;
      })
      .map(entity -> this.conversionService.convert(entity, Invoice.class))
      .orElseThrow(() -> new InvoiceNotFoundProblem(invoice.id()));
  }

  @Override
  public PaymentDetailsType getPaymentDetails(InvoiceId invoiceId, AccountId accountId) {
    throw new UnsupportedInvoiceOperationProblem(invoiceId);
  }

  @Override
  public PaymentResultType payInvoice(InvoiceId invoiceId, AccountId senderAccountId, Optional<PayInvoiceRequest> payInvoiceRequest) {
    throw new UnsupportedInvoiceOperationProblem(invoiceId);
  }

  public void onPayment(Payment payment) {

    List<Invoice> invoices = invoicesRepository.findAllInvoicesByCorrelationId(payment.correlationId());

    if (invoices.isEmpty()) {
      throw new IllegalArgumentException("Could not find invoice by correlation ID.");
    }

    invoices
      .forEach(invoice -> {
        // See if we have already processed this payment for this invoice
        Optional<Payment> existingPayment = paymentsRepository.findPaymentByPaymentIdAndInvoicePrimaryKey(payment.paymentId(), invoice.primaryKey());

        // If we haven't, do it
        if (!existingPayment.isPresent()) {
          Invoice updatedInvoice = Invoice.builder()
            .from(invoice)
            .received(invoice.received().plus(payment.amount()))
            .build();
          this.updateInvoice(updatedInvoice, AccountId.of(invoice.accountId()));

          Payment paymentToSave = Payment.builder()
            .from(payment)
            .invoicePrimaryKey(invoice.primaryKey())
            .build();
          paymentsRepository.savePayment(paymentToSave);
        }
      });
  }

  protected boolean isForThisWallet(HttpUrl invoiceUrl) {
    HttpUrl issuer = openPaymentsSettingsSupplier.get().metadata().issuer();
    if (issuer.host().equals("localhost")) {
      return issuer.host().equals(invoiceUrl.host()) && issuer.port() == invoiceUrl.port();
    }

    return issuer.host().equals(invoiceUrl.host());
  }


  protected UnsignedLong min(UnsignedLong first, UnsignedLong second) {
    if (first.compareTo(second) < 0) {
      return first;
    }

    return second;
  }
}
