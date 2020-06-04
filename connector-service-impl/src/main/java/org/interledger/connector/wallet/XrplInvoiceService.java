package org.interledger.connector.wallet;

import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentId;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.persistence.repositories.PaymentsRepository;
import org.interledger.openpayments.events.XrpPaymentCompletedEvent;
import org.interledger.openpayments.events.XrplTransaction;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;

import java.util.function.Supplier;

public class XrplInvoiceService extends AbstractInvoiceService<XrpPayment, XrpPaymentDetails> implements XrplPaymentEventHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
}
