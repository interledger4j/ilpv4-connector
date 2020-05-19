package org.interledger.connector.wallet;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.events.StreamPaymentClosedEvent;
import org.interledger.connector.opa.model.IlpPaymentDetails;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PayInvoiceRequest;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentDetails;
import org.interledger.connector.opa.model.PaymentNetwork;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.persistence.repositories.InvoicesRepository;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.springframework.core.convert.ConversionService;

import java.util.Optional;
import java.util.function.Supplier;

public class IlpInvoiceService extends AbstractInvoiceService<StreamPayment, IlpPaymentDetails> implements StreamPaymentEventHandler {
  private final IlpOpenPaymentsPaymentService ilpOpenPaymentsPaymentService;

  public IlpInvoiceService(
    InvoicesRepository invoicesRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsClient openPaymentsClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    IlpOpenPaymentsPaymentService ilpOpenPaymentsPaymentService,
    EventBus eventBus
  ) {
    super(
      invoicesRepository,
      conversionService,
      invoiceFactory,
      openPaymentsClient,
      openPaymentsSettingsSupplier,
      eventBus
    );
    this.ilpOpenPaymentsPaymentService = ilpOpenPaymentsPaymentService;
  }

  @Override
  public IlpPaymentDetails getPaymentDetails(InvoiceId invoiceId) {
    final Invoice invoice = this.getInvoiceById(invoiceId);

    final HttpUrl invoiceUrl = invoice.invoiceUrl()
      .orElseThrow(() -> new IllegalStateException("Invoice should have a location after creation."));

    if (!isForThisWallet(invoiceUrl)) {
      return openPaymentsClient.getIlpInvoicePaymentDetails(invoiceUrl.uri());
    }

    return ilpOpenPaymentsPaymentService.getPaymentDetails(invoice);
  }

  @Override
  public StreamPayment payInvoice(InvoiceId invoiceId, AccountId senderAccountId, Optional<PayInvoiceRequest> payInvoiceRequest) {
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
    UnsignedLong amountToPay =
      min(amountLeftToSend, payInvoiceRequest.orElse(PayInvoiceRequest.builder().build()).amount());

    return ilpOpenPaymentsPaymentService.payInvoice(ilpPaymentDetails, senderAccountId, amountToPay, invoiceId);
  }

  @Override
  @Subscribe
  public void onPaymentCompleted(StreamPaymentClosedEvent paymentCompletedEvent) {
    this.onPayment(Payment.builder().build())
  }
}
