package org.interledger.connector.wallet;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.problems.InvoiceNotFoundProblem;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.connector.persistence.repositories.InvoicesRepository;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import feign.FeignException;
import okhttp3.HttpUrl;
import org.springframework.core.convert.ConversionService;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractInvoiceService<PaymentResultType, PaymentDetailsType> implements InvoiceService<PaymentResultType, PaymentDetailsType> {
  protected final InvoicesRepository invoicesRepository;
  protected final ConversionService conversionService;
  protected final InvoiceFactory invoiceFactory;
  protected final OpenPaymentsClient openPaymentsClient;
  protected final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;

  public AbstractInvoiceService(
    final InvoicesRepository invoicesRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsClient openPaymentsClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    EventBus eventBus
  ) {
    this.invoicesRepository = Objects.requireNonNull(invoicesRepository);
    this.conversionService = Objects.requireNonNull(conversionService);
    this.invoiceFactory = Objects.requireNonNull(invoiceFactory);
    this.openPaymentsClient = Objects.requireNonNull(openPaymentsClient);
    this.openPaymentsSettingsSupplier = Objects.requireNonNull(openPaymentsSettingsSupplier);
    eventBus.register(this);
  }

  @Override
  public Invoice getInvoiceById(InvoiceId invoiceId) {
    Objects.requireNonNull(invoiceId);

    // TODO(bridge): If the invoice hasn't been paid, ask the bridge for any payments that have been sent for the invoice
    //  and update the invoice received amount.
    return invoicesRepository.findInvoiceByInvoiceId(invoiceId)
      .orElseThrow(() -> new InvoiceNotFoundProblem(invoiceId));
  }

  @Override
  public Invoice syncInvoice(HttpUrl invoiceUrl) {
    Objects.requireNonNull(invoiceUrl);

    // See if we have that invoice already
    return invoicesRepository.findInvoiceByInvoiceUrl(invoiceUrl)
      .map(i -> {
        // TODO(bridge): When the sender already has the invoice, we should throw an error and return a 409(?), which
        //  will let the client know they can just do a get on the invoice ID.

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
        .addPathSegment("accounts")
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

  public Optional<Invoice> onPayment(XrpPayment xrpPayment) {
    return Optional.empty();
  }

  public Optional<Invoice> onPayment(Payment payment) {
    InvoiceId invoiceIdFromCorrelationId = payment.correlationId()
      .map(InvoiceId::of)
      .orElseThrow(() -> new IllegalArgumentException("StreamPayment did not have a correlationId.  Unable to update invoice for payment."));

    Invoice existingInvoice = this.getInvoiceById(invoiceIdFromCorrelationId);

    if (!existingInvoice.assetCode().equals(payment.assetCode())) {
      throw new IllegalStateException(String.format("Invoice asset code was different than the StreamPayment asset code." +
        "Unable to accurately credit invoice. Invoice assetCode: %s ; Payment assetCode: %s", existingInvoice.assetCode(), payment.assetCode()));
    }

    Invoice updatedInvoice = Invoice.builder()
      .from(existingInvoice)
      .received(existingInvoice.received().plus(payment.deliveredAmount()))
      .build();

    return Optional.of(this.updateInvoice(updatedInvoice));
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
