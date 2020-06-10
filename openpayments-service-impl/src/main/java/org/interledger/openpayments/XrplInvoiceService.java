package org.interledger.openpayments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.persistence.repositories.PaymentsRepository;
import org.interledger.openpayments.config.OpenPaymentsSettings;
import org.interledger.openpayments.events.XrpPaymentCompletedEvent;
import org.interledger.openpayments.xrpl.XrplTransaction;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.Hashing;
import com.google.common.primitives.UnsignedLong;
import io.vavr.control.Either;
import okhttp3.HttpUrl;
import org.interleger.openpayments.PaymentSystemFacade;
import org.interleger.openpayments.PaymentSystemFacadeFactory;
import org.interleger.openpayments.XrplPaymentEventHandler;
import org.interleger.openpayments.client.OpenPaymentsProxyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class XrplInvoiceService extends AbstractInvoiceService<XrplTransaction, XrpPaymentDetails> implements XrplPaymentEventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(XrplInvoiceService.class);

  private final PaymentSystemFacadeFactory paymentSystemFacadeFactory;

  public XrplInvoiceService(
    InvoicesRepository invoicesRepository,
    PaymentsRepository paymentsRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsProxyClient openPaymentsProxyClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    EventBus eventBus,
    PaymentSystemFacadeFactory paymentSystemFacadeFactory) {
    super(
      invoicesRepository,
      paymentsRepository,
      conversionService,
      invoiceFactory,
      openPaymentsProxyClient,
      openPaymentsSettingsSupplier,
      eventBus
    );
    this.paymentSystemFacadeFactory = paymentSystemFacadeFactory;
    eventBus.register(this);
  }

  @Override
  public XrpPaymentDetails getPaymentDetails(InvoiceId invoiceId, AccountId accountId) {
    final Invoice invoice = this.getInvoice(invoiceId, accountId);

    final HttpUrl invoiceUrl = invoice.receiverInvoiceUrl();

    final Invoice receiverInvoice = openPaymentsProxyClient.getInvoice(invoiceUrl.uri());

    PaymentSystemFacade<XrplTransaction, XrpPaymentDetails> paymentFacade =
      paymentSystemFacadeFactory.get(XrplTransaction.class, XrpPaymentDetails.class)
        .orElseThrow(() -> new UnsupportedOperationException("No provider for XrpPayment type"));
    return paymentFacade.getPaymentDetails(receiverInvoice);
  }

  @Override
  public XrplTransaction payInvoice(InvoiceId invoiceId, AccountId senderAccountId, Optional<PayInvoiceRequest> payInvoiceRequest, Optional<String> paymentCompleteRedirectUrl) throws
    UserAuthorizationRequiredException
    {
    final Invoice invoice = this.getInvoice(invoiceId, senderAccountId);
    final HttpUrl invoiceUrl = invoice.receiverInvoiceUrl();
    try {
      Either<UserAuthorizationRequiredException, XrplTransaction> result =
        paymentSystemFacadeFactory.get(XrplTransaction.class, XrpPaymentDetails.class)
        .map(facade -> {
          try {
            XrplTransaction trx = facade.payInvoice(getPaymentDetails(invoiceId, senderAccountId),
              senderAccountId,
              payInvoiceRequest.get().amount(),
              invoice.correlationId(),
              paymentCompleteRedirectUrl
            );
            return Either.<UserAuthorizationRequiredException, XrplTransaction>right(trx);
          } catch (UserAuthorizationRequiredException e) {
            return Either.<UserAuthorizationRequiredException, XrplTransaction>left(e);
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e); // TODO what to do here
          }
        })
        .orElseThrow(() -> new UnsupportedOperationException("No provider for XrpPayment type"));
      return result.getOrElseThrow((ex) -> ex);
    } catch (Exception e) {
      LOGGER.error("failed to payinvoice for invoiceId {}", invoiceId, e);
      // FIXME what to do here?
      throw new RuntimeException("Payment failed and we don't know what to do about it", e);
    }

  }

  /**
   * ONLY FOR RECEIVING
   *
   * @param xrpPaymentCompletedEvent
   */
  @Override
  @Subscribe
  public void onPaymentCompleted(XrpPaymentCompletedEvent xrpPaymentCompletedEvent) {
    XrplTransaction transaction = xrpPaymentCompletedEvent.payment();
    transaction.invoiceMemoCorrelationId().ifPresent(correlationId -> {
      Payment payment = Payment.builder()
        .amount(transaction.amount())
        .correlationId(correlationId)
        .paymentId(PaymentId.of(transaction.hash()))
        .createdAt(transaction.createdAt())
        .modifiedAt(transaction.modifiedAt())
        .sourceAddress(transaction.account() + transaction.sourceTag())
        .destinationAddress(transaction.destination() + transaction.destinationTag())
        .amount(transaction.amount())
        .denomination(transaction.denomination())
        .build();
      this.onPayment(payment);
    });
  }

  @Override
  public Class<XrpPaymentDetails> getRequestType() {
    return XrpPaymentDetails.class;
  }

  @Override
  public Class<XrplTransaction> getResultType() {
    return XrplTransaction.class;
  }

  private String transactionHash(InvoiceId invoiceId) {
    return Hashing.sha256()
      .hashString(invoiceId.value(), StandardCharsets.US_ASCII)
      .toString();
  }

}
