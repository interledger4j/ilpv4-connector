package org.interledger.connector.wallet;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.PaymentDetailsService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceFactory;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PaymentDetails;
import org.interledger.connector.opa.model.PaymentNetwork;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.problems.InvoiceNotFoundProblem;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.connector.persistence.repositories.InvoicesRepository;

import feign.FeignException;
import okhttp3.HttpUrl;
import org.springframework.core.convert.ConversionService;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class DefaultInvoiceService implements InvoiceService {

  private final InvoicesRepository invoicesRepository;
  private final ConversionService conversionService;
  private final InvoiceFactory invoiceFactory;
  private final OpenPaymentsClient openPaymentsClient;
  private final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;
  private final PaymentDetailsService xrpPaymentDetailsService;
  private final PaymentDetailsService ilpPaymentDetailsService;

  public DefaultInvoiceService(
    final InvoicesRepository invoicesRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsClient openPaymentsClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    PaymentDetailsService xrpPaymentDetailsService,
    PaymentDetailsService ilpPaymentDetailsService
  ) {
    this.invoicesRepository = Objects.requireNonNull(invoicesRepository);
    this.conversionService = Objects.requireNonNull(conversionService);
    this.invoiceFactory = Objects.requireNonNull(invoiceFactory);
    this.openPaymentsClient = Objects.requireNonNull(openPaymentsClient);
    this.openPaymentsSettingsSupplier = Objects.requireNonNull(openPaymentsSettingsSupplier);
    this.xrpPaymentDetailsService = Objects.requireNonNull(xrpPaymentDetailsService);
    this.ilpPaymentDetailsService = Objects.requireNonNull(ilpPaymentDetailsService);
  }

  @Override
  public Invoice getInvoiceById(InvoiceId invoiceId) {
    Objects.requireNonNull(invoiceId);

    Invoice invoice = invoicesRepository.findInvoiceByInvoiceId(invoiceId)
      .orElseThrow(() -> new InvoiceNotFoundProblem(invoiceId));

    // TODO: throw an exception if the invoice hasn't been given a name
    HttpUrl invoiceUrl = invoice.invoiceUrl().get();

    // The sender and receiver wallets could be the same wallet, so the invoice and the invoice receipt could be
    // the same record. In this case, we should just return what we have.
    if (isForThisWallet(invoiceUrl)) {
      return invoice;
    }

    // Otherwise, we can assume that some other wallet is the invoice owner.
    try {
      // Look at our own records. If, from our view, the invoice has been paid, no need to reach out to the receiver to
      // update it.
      if (invoice.isPaid()) {
        return invoice;
      } else {
        throw new Exception("Local invoice receipt has not been fully paid. Request an updated invoice from the invoice owner.");
      }
    } catch (Exception e) {
      // If our copy of the invoice hasn't been paid, or we don't have a record of that invoice, try
      // to reach out to the invoice owner to update/create our record
      try {
        Invoice invoiceOnReceiver = openPaymentsClient.getInvoice(invoiceUrl.uri());
        return this.updateOrCreateInvoice(invoiceOnReceiver);
      } catch (FeignException fe) {
        if (fe.status() == 404) {
          throw new InvoiceNotFoundProblem("Original invoice was not found in invoice owner's records.", invoiceId);
        }

        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public Invoice createInvoice(Invoice invoice) {
    Objects.requireNonNull(invoice);

    Invoice invoiceWithUrl = invoiceFactory.construct(invoice);
    // This creation was meant for another...

    HttpUrl invoiceUrl = invoiceWithUrl.invoiceUrl().get();

    if (!isForThisWallet(invoiceUrl)) {
      HttpUrl createUrl = new HttpUrl.Builder()
        .scheme(invoiceUrl.scheme())
        .host(invoiceUrl.host())
        .addPathSegment(invoice.accountId())
        .addPathSegment("invoices")
        .build();
      Invoice invoiceCreatedOnReceiver = openPaymentsClient.createInvoice(createUrl.uri(), invoice);
      return invoicesRepository.saveInvoice(invoiceCreatedOnReceiver);
    }

    return invoicesRepository.saveInvoice(invoiceWithUrl);
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
  public Invoice updateOrCreateInvoice(Invoice invoice) {
    try {
      return this.updateInvoice(invoice);
    } catch (InvoiceNotFoundProblem e) {
      return this.createInvoice(invoice);
    }
  }

  @Override
  public PaymentDetails getPaymentDetails(InvoiceId invoiceId) {
    final Invoice invoice = this.getInvoiceById(invoiceId);

    final HttpUrl invoiceUrl = invoice.invoiceUrl()
      .orElseThrow(() -> new IllegalStateException("Invoice should have a location after creation."));

    if (!isForThisWallet(invoiceUrl)) {
      return this.proxyPaymentDetails(invoice);
    }

    if (invoice.paymentNetwork().equals(PaymentNetwork.XRPL)) {
      return xrpPaymentDetailsService.getPaymentDetails(invoice);
    } else {
      return ilpPaymentDetailsService.getPaymentDetails(invoice);
    }
  }

  private PaymentDetails proxyPaymentDetails(Invoice invoice) {
    if (invoice.paymentNetwork().equals(PaymentNetwork.XRPL)) {
      return openPaymentsClient.getXrpInvoicePaymentDetails(invoice.invoiceUrl().get().uri());
    } else {
      return openPaymentsClient.getIlpInvoicePaymentDetails(invoice.invoiceUrl().get().uri());
    }
  }

  @Override
  public Optional<Invoice> onPayment(XrpPayment xrpPayment) {
    return Optional.empty();
  }

  @Override
  public Optional<Invoice> onPayment(StreamPayment streamPayment) {
    return Optional.empty();
  }

  private boolean isForThisWallet(HttpUrl invoiceUrl) {
    HttpUrl issuer = openPaymentsSettingsSupplier.get().metadata().issuer();
    if (issuer.host().equals("localhost")) {
      return issuer.host().equals(invoiceUrl.host()) && issuer.port() == invoiceUrl.port();
    }

    return issuer.host().equals(invoiceUrl.host());
  }
}
