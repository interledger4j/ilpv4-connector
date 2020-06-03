package org.interledger.connector.wallet;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.PaymentSystemFacade;
import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.Memo;
import org.interledger.connector.opa.model.MemoWrapper;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PayInvoiceRequest;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentId;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.connector.opa.model.XrplTransaction;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.persistence.repositories.PaymentsRepository;
import org.interledger.openpayments.events.XrpPaymentCompletedEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.Hashing;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.springframework.core.convert.ConversionService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

public class XrplInvoiceService extends AbstractInvoiceService<XrpPayment, XrpPaymentDetails> implements XrplPaymentEventHandler {

  private static final Logger LOGGER = getLogger(XrplInvoiceService.class);

  private final PaymentSystemFacade<XrpPayment, XrpPaymentDetails> xrpPaymentSystemFacade;

  public XrplInvoiceService(
    InvoicesRepository invoicesRepository,
    PaymentsRepository paymentsRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsProxyClient openPaymentsProxyClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    EventBus eventBus,
    PaymentSystemFacade<XrpPayment, XrpPaymentDetails> xrpPaymentSystemFacade) {
    super(
      invoicesRepository,
      paymentsRepository,
      conversionService,
      invoiceFactory,
      openPaymentsProxyClient,
      openPaymentsSettingsSupplier,
      eventBus
    );
    this.xrpPaymentSystemFacade = xrpPaymentSystemFacade;
  }

  @Override
  public XrpPaymentDetails getPaymentDetails(InvoiceId invoiceId, AccountId accountId) {
    final Invoice invoice = this.getInvoiceById(invoiceId, accountId);

    final HttpUrl invoiceUrl = invoice.invoiceUrl()
      .orElseThrow(() -> new IllegalStateException("Invoice should have a location after creation."));

    final Invoice receiverInvoice = openPaymentsProxyClient.getInvoice(invoiceUrl.uri());

    return xrpPaymentSystemFacade.getPaymentDetails(receiverInvoice);
  }

  @Override
  public XrpPayment payInvoice(InvoiceId invoiceId, AccountId senderAccountId, Optional<PayInvoiceRequest> payInvoiceRequest) {
    final Invoice invoice = this.getInvoiceById(invoiceId, senderAccountId);
    final HttpUrl invoiceUrl = invoice.invoiceUrl()
      .orElseThrow(() -> new IllegalStateException("Invoice should have a location after creation."));

    if (!isForThisWallet(invoiceUrl)) {
      // FIXME
      throw new UnsupportedOperationException("remote invoice payments not yet implemented");
    } else {
      try {
        return xrpPaymentSystemFacade.payInvoice(getPaymentDetails(invoiceId, senderAccountId),
          senderAccountId,
          payInvoiceRequest.get().amount(),
          CorrelationId.of(transactionHash(invoiceId)));
      } catch (Exception e) {
        LOGGER.error("failed to payinvoice for invoiceId {}", invoiceId, e);
        // FIXME what to do here?
        throw new RuntimeException("Payment failed and we don't know what to do about it", e);
      }
    }
  }

  /**
   * ONLY FOR RECEIVING
   * @param xrpPaymentCompletedEvent
   */
  @Override
  @Subscribe
  public void onPaymentCompleted(XrpPaymentCompletedEvent xrpPaymentCompletedEvent) {
    XrplTransaction transaction = xrpPaymentCompletedEvent.payment();
    Optional<Memo> invoiceMemo = transaction.memos().stream()
      .map(MemoWrapper::memo)
      .filter(memo -> memo.memoType().equals("INVOICE_PAYMENT")) // FIXME: Define a memoType constant
      .findFirst();

    invoiceMemo.ifPresent(memo -> {
      Payment payment = Payment.builder()
        .amount(transaction.amount())
        .correlationId(CorrelationId.of(memo.memoData())) // FIXME: may need to decode hex
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

  private String transactionHash(InvoiceId invoiceId) {
    return Hashing.sha256()
      .hashString(invoiceId.value(), StandardCharsets.US_ASCII)
      .toString();
  }

}
