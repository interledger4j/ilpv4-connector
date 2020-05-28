package org.interledger.connector.wallet;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentId;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.persistence.repositories.PaymentsRepository;
import org.interledger.openpayments.events.Memo;
import org.interledger.openpayments.events.MemoWrapper;
import org.interledger.openpayments.events.XrpPaymentCompletedEvent;
import org.interledger.openpayments.events.XrplTransaction;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.core.convert.ConversionService;

import java.util.Optional;
import java.util.function.Supplier;

public class XrplInvoiceService extends AbstractInvoiceService<XrpPayment, XrpPaymentDetails> implements XrplPaymentEventHandler {

  public XrplInvoiceService(
    InvoicesRepository invoicesRepository,
    PaymentsRepository paymentsRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsProxyClient openPaymentsProxyClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
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
      .filter(memo -> memo.memoType().equals("?????????")) // FIXME: Define a memoType constant
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
}
