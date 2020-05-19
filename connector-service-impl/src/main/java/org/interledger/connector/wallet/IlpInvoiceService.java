package org.interledger.connector.wallet;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.events.StreamPaymentClosedEvent;
import org.interledger.connector.opa.PaymentSystemFacade;
import org.interledger.connector.opa.model.Denomination;
import org.interledger.connector.opa.model.IlpPaymentDetails;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PayInvoiceRequest;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentId;
import org.interledger.connector.opa.model.PaymentNetwork;
import org.interledger.connector.opa.model.PaymentType;
import org.interledger.connector.opa.model.problems.InvoicePaymentProblem;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.persistence.repositories.InvoicesRepository;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.springframework.core.convert.ConversionService;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class IlpInvoiceService extends AbstractInvoiceService<StreamPayment, IlpPaymentDetails> implements StreamPaymentEventHandler {

  private final PaymentSystemFacade<StreamPayment, IlpPaymentDetails> ilpPaymentSystemFacade;

  public IlpInvoiceService(
    InvoicesRepository invoicesRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsClient openPaymentsClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    PaymentSystemFacade<StreamPayment, IlpPaymentDetails> ilpPaymentSystemFacade,
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
    this.ilpPaymentSystemFacade = ilpPaymentSystemFacade;
  }

  @Override
  public IlpPaymentDetails getPaymentDetails(InvoiceId invoiceId) {
    final Invoice invoice = this.getInvoiceById(invoiceId);

    final HttpUrl invoiceUrl = invoice.invoiceUrl()
      .orElseThrow(() -> new IllegalStateException("Invoice should have a location after creation."));

    if (!isForThisWallet(invoiceUrl)) {
      return openPaymentsClient.getIlpInvoicePaymentDetails(invoiceUrl.uri());
    }

    return ilpPaymentSystemFacade.getPaymentDetails(invoice);
  }

  @Override
  public StreamPayment payInvoice(InvoiceId invoiceId, AccountId senderAccountId, Optional<PayInvoiceRequest> payInvoiceRequest) {
    final Invoice invoice = this.getInvoiceById(invoiceId);

    if (!invoice.paymentNetwork().equals(PaymentNetwork.ILP)) {
      throw new IllegalStateException("Unable to pay invoice from Open Payment Server over non-ILP payment network.");
    }

    final HttpUrl invoiceUrl = invoice.invoiceUrl()
      .orElseThrow(() -> new IllegalStateException("Invoice should have a location after creation."));

    IlpPaymentDetails ilpPaymentDetails;

    if (!isForThisWallet(invoiceUrl)) {
      ilpPaymentDetails = openPaymentsClient.getIlpInvoicePaymentDetails(invoiceUrl.uri());
    } else {
      ilpPaymentDetails = ilpPaymentSystemFacade.getPaymentDetails(invoice);
    }

    UnsignedLong amountLeftToSend = invoice.amount().minus(invoice.received());
    UnsignedLong amountToPay =
      min(amountLeftToSend, payInvoiceRequest.orElse(PayInvoiceRequest.builder().build()).amount());

    try {
      return ilpPaymentSystemFacade.payInvoice(ilpPaymentDetails, senderAccountId, amountToPay, invoiceId);
    } catch (ExecutionException | InterruptedException e) {
      throw new InvoicePaymentProblem(e.getMessage(), invoiceId);
    }

  }

  @Override
  @Subscribe
  public void onPaymentCompleted(StreamPaymentClosedEvent paymentCompletedEvent) {
    StreamPayment streamPayment = paymentCompletedEvent.streamPayment();
    if (streamPayment.correlationId().isPresent()) {
      Payment payment = Payment.builder()
        .amount(streamPayment.deliveredAmount())
        .correlationId(streamPayment.correlationId().get())
        .paymentId(PaymentId.of(streamPayment.streamPaymentId()))
        .createdAt(streamPayment.createdAt())
        .modifiedAt(streamPayment.modifiedAt())
        .sourceAddress(streamPayment.sourceAddress().toString())
        .destinationAddress(streamPayment.destinationAddress().toString())
        .amount(streamPayment.deliveredAmount())
        .denomination(Denomination.builder()
          .assetCode(streamPayment.assetCode())
          .assetScale(streamPayment.assetScale())
          .build())
        .type(PaymentType.valueOf(streamPayment.type().name()))
        .build();
      this.onPayment(payment);
    }
  }
}
