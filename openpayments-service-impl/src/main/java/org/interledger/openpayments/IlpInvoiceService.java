package org.interledger.openpayments;

import org.interledger.connector.events.StreamPaymentClosedEvent;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.persistence.repositories.PaymentsRepository;
import org.interledger.openpayments.config.OpenPaymentsSettings;
import org.interledger.openpayments.problems.InvoicePaymentProblem;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.interleger.openpayments.PaymentSystemFacade;
import org.interleger.openpayments.StreamPaymentEventHandler;
import org.interleger.openpayments.client.OpenPaymentsProxyClient;
import org.springframework.core.convert.ConversionService;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class IlpInvoiceService extends AbstractInvoiceService<StreamPayment, IlpPaymentDetails> implements StreamPaymentEventHandler {

  private final PaymentSystemFacade<StreamPayment, IlpPaymentDetails> ilpPaymentSystemFacade;

  public IlpInvoiceService(
    InvoicesRepository invoicesRepository,
    PaymentsRepository paymentsRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsProxyClient openPaymentsProxyClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    PaymentSystemFacade<StreamPayment, IlpPaymentDetails> ilpPaymentSystemFacade,
    EventBus eventBus
  ) {
    super(
      invoicesRepository,
      paymentsRepository,
      conversionService,
      invoiceFactory,
      openPaymentsProxyClient,
      openPaymentsSettingsSupplier,
      eventBus
    );
    this.ilpPaymentSystemFacade = ilpPaymentSystemFacade;
  }

  @Override
  public IlpPaymentDetails getPaymentDetails(InvoiceId invoiceId, PayIdAccountId payIdAccountId) {
    final Invoice invoice = this.getInvoice(invoiceId, payIdAccountId);

    final HttpUrl invoiceUrl = invoice.receiverInvoiceUrl();

    if (!isForThisWallet(invoiceUrl)) {
      return openPaymentsProxyClient.getIlpInvoicePaymentDetails(invoiceUrl.uri());
    }

    return ilpPaymentSystemFacade.getPaymentDetails(invoice);
  }

  @Override
  public StreamPayment payInvoice(InvoiceId invoiceId, PayIdAccountId senderPayIdAccountId, Optional<PayInvoiceRequest> payInvoiceRequest) {
    final Invoice invoice = this.getInvoice(invoiceId, senderPayIdAccountId);

    final HttpUrl invoiceUrl = invoice.receiverInvoiceUrl();

    IlpPaymentDetails ilpPaymentDetails;

    if (!isForThisWallet(invoiceUrl)) {
      ilpPaymentDetails = openPaymentsProxyClient.getIlpInvoicePaymentDetails(invoiceUrl.uri());
    } else {
      ilpPaymentDetails = ilpPaymentSystemFacade.getPaymentDetails(invoice);
    }

    UnsignedLong amountLeftToSend = invoice.amount().minus(invoice.received());
    UnsignedLong amountToPay =
      min(amountLeftToSend, payInvoiceRequest.orElse(PayInvoiceRequest.builder().build()).amount());

    try {
      return ilpPaymentSystemFacade.payInvoice(ilpPaymentDetails, senderPayIdAccountId, amountToPay, invoice.correlationId());
    } catch (UserAuthorizationRequiredException e) {
      // should not happen on ILP payments
      throw new InvoicePaymentProblem(e.getMessage(), invoiceId);
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
        .amount(UnsignedLong.valueOf(streamPayment.amount().abs()))
        .correlationId(CorrelationId.of(streamPayment.correlationId().get()))
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
        .build();
      this.onPayment(payment);
    }
  }

  @Override
  public Class<StreamPayment> getResultType() {
    return StreamPayment.class;
  }

  @Override
  public Class<IlpPaymentDetails> getRequestType() {
    return IlpPaymentDetails.class;
  }
}
