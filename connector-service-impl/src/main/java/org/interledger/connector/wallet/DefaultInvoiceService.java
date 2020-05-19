package org.interledger.connector.wallet;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.events.StreamPaymentClosedEvent;
import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.OpenPaymentsPaymentService;
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
import org.interledger.stream.SendMoneyResult;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.UnsignedLong;
import feign.FeignException;
import okhttp3.HttpUrl;
import org.springframework.core.convert.ConversionService;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

// TODO: Split this into an ILPInvoiceService and an XRPInvoiceService?
public class DefaultInvoiceService implements InvoiceService {

  private final InvoicesRepository invoicesRepository;
  private final ConversionService conversionService;
  private final InvoiceFactory invoiceFactory;
  private final OpenPaymentsClient openPaymentsClient;
  private final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;
  private final OpenPaymentsPaymentService<SendMoneyResult> xrpOpenPaymentsPaymentService;
  private final OpenPaymentsPaymentService<StreamPayment> ilpOpenPaymentsPaymentService;

  public DefaultInvoiceService(
    final InvoicesRepository invoicesRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsClient openPaymentsClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    OpenPaymentsPaymentService<SendMoneyResult> xrpOpenPaymentsPaymentService,
    OpenPaymentsPaymentService<StreamPayment> ilpOpenPaymentsPaymentService,
    EventBus eventBus) {
    this.invoicesRepository = Objects.requireNonNull(invoicesRepository);
    this.conversionService = Objects.requireNonNull(conversionService);
    this.invoiceFactory = Objects.requireNonNull(invoiceFactory);
    this.openPaymentsClient = Objects.requireNonNull(openPaymentsClient);
    this.openPaymentsSettingsSupplier = Objects.requireNonNull(openPaymentsSettingsSupplier);
    this.xrpOpenPaymentsPaymentService = Objects.requireNonNull(xrpOpenPaymentsPaymentService);
    this.ilpOpenPaymentsPaymentService = Objects.requireNonNull(ilpOpenPaymentsPaymentService);
    eventBus.register(this);
  }

  @Override
  public Invoice getInvoiceById(InvoiceId invoiceId) {
    Objects.requireNonNull(invoiceId);

    return invoicesRepository.findInvoiceByInvoiceId(invoiceId)
      .orElseThrow(() -> new InvoiceNotFoundProblem(invoiceId));
  }

  @Override
  public Invoice getOrSyncInvoice(HttpUrl invoiceUrl) {
    Objects.requireNonNull(invoiceUrl);

    // See if we have that invoice already
    return invoicesRepository.findInvoiceByInvoiceUrl(invoiceUrl)
      .map(i -> {
        // if we do:

        // The sender and receiver wallets could be the same wallet, so the invoice and the invoice receipt could be
        // the same record. In this case, we should just return what we have.
        if (isForThisWallet(invoiceUrl)) {
          return i;
        }

        // Otherwise, we can assume that some other wallet is the invoice owner.
        // Look at our own records. If, from our view, the invoice has been paid, no need to reach out to the receiver to
        // update it.
        if (i.isPaid()) {
          return i;
        } else {
          // If our copy of the invoice hasn't been paid, try
          // to reach out to the invoice owner to update our record
          Invoice invoiceOnReceiver = this.getRemoteInvoice(invoiceUrl);
          return this.updateInvoice(invoiceOnReceiver);
        }
      })
      .orElseGet(() -> {
        Invoice invoiceOnReceiver = this.getRemoteInvoice(invoiceUrl);
        return invoicesRepository.saveInvoice(invoiceOnReceiver);
      });
  }

  private Invoice getRemoteInvoice(HttpUrl invoiceUrl) {
    try {
      return openPaymentsClient.getInvoice(invoiceUrl.uri());
    } catch (FeignException e) {
      if (e.status() == 404) {
        throw new InvoiceNotFoundProblem("Original invoice was not found in invoice owner's records.", invoiceUrl);
      }

      throw e;
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
        .port(invoiceUrl.port())
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
      if (invoice.paymentNetwork().equals(PaymentNetwork.XRPL)) {
        return openPaymentsClient.getXrpInvoicePaymentDetails(invoiceUrl.uri());
      } else {
        return openPaymentsClient.getIlpInvoicePaymentDetails(invoiceUrl.uri());
      }
    }

    if (invoice.paymentNetwork().equals(PaymentNetwork.XRPL)) {
      return xrpOpenPaymentsPaymentService.getPaymentDetails(invoice);
    } else {
      return ilpOpenPaymentsPaymentService.getPaymentDetails(invoice);
    }
  }

  @Override
  public StreamPayment payInvoice(InvoiceId invoiceId, AccountId senderAccountId, String bearerToken) {
    final Invoice invoice = this.getInvoiceById(invoiceId);

    if (!invoice.paymentNetwork().equals(PaymentNetwork.ILP)) {
      throw new IllegalStateException("Unable to pay invoice from Open Payment Server over non-ILP payment network.");
    }

    final HttpUrl invoiceUrl = invoice.invoiceUrl()
      .orElseThrow(() -> new IllegalStateException("Invoice should have a location after creation."));

    PaymentDetails ilpPaymentDetails;

    if (!isForThisWallet(invoiceUrl)) {
      ilpPaymentDetails = openPaymentsClient.getIlpInvoicePaymentDetails(invoiceUrl.uri());
    } else {
      ilpPaymentDetails = ilpOpenPaymentsPaymentService.getPaymentDetails(invoice);
    }

    UnsignedLong amountLeftToSend = invoice.amount().minus(invoice.received());
    try {
      return ilpOpenPaymentsPaymentService.payInvoice(ilpPaymentDetails, senderAccountId, amountLeftToSend, invoiceId);
    } catch (InterruptedException | ExecutionException e) {
      // TODO: Throw an invoice problem
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<Invoice> onPayment(XrpPayment xrpPayment) {
    return Optional.empty();
  }

  @Override
  public Optional<Invoice> onPayment(StreamPayment streamPayment) {
    InvoiceId invoiceIdFromCorrelationId = streamPayment.correlationId()
      .map(InvoiceId::of)
      .orElseThrow(() -> new IllegalArgumentException("StreamPayment did not have a correlationId.  Unable to update invoice for payment."));

    Invoice existingInvoice = this.getInvoiceById(invoiceIdFromCorrelationId);

    if (!existingInvoice.assetCode().equals(streamPayment.assetCode())) {
      throw new IllegalStateException(String.format("Invoice asset code was different than the StreamPayment asset code." +
        "Unable to accurately credit invoice. Invoice assetCode: %s ; Payment assetCode: %s", existingInvoice.assetCode(), streamPayment.assetCode()));
    }

    Invoice updatedInvoice = Invoice.builder()
      .from(existingInvoice)
      .received(existingInvoice.received().plus(streamPayment.deliveredAmount()))
      .build();

    return Optional.of(this.updateInvoice(updatedInvoice));
  }

  /**
   * Handler that is called whenever a {@link StreamPaymentClosedEvent} is posted to the event bus.
   *
   * @param streamPaymentClosedEvent A {@link StreamPaymentClosedEvent}.
   */
  @Subscribe
  private void onStreamPaymentClosed(final StreamPaymentClosedEvent streamPaymentClosedEvent) {
    Objects.requireNonNull(streamPaymentClosedEvent);
    StreamPayment payment = streamPaymentClosedEvent.streamPayment();
    if (payment.correlationId().isPresent()) {
      onPayment(payment);
    }
  }

  private boolean isForThisWallet(HttpUrl invoiceUrl) {
    HttpUrl issuer = openPaymentsSettingsSupplier.get().metadata().issuer();
    if (issuer.host().equals("localhost")) {
      return issuer.host().equals(invoiceUrl.host()) && issuer.port() == invoiceUrl.port();
    }

    return issuer.host().equals(invoiceUrl.host());
  }

}
