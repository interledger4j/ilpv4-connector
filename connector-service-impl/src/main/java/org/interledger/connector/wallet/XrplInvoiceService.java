package org.interledger.connector.wallet;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.Payment;
import org.interledger.connector.opa.model.PaymentId;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
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
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsProxyClient openPaymentsProxyClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    EventBus eventBus
  ) {
    super(
      invoicesRepository,
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
    if (transaction.invoiceHash() != null) {
      Payment payment = Payment.builder()
        .accountId(AccountId.of(transaction.destination() + transaction.destinationTag()))
        .amount(transaction.amount())
        .correlationId(Optional.ofNullable(transaction.invoiceHash()))
        .paymentId(PaymentId.of(transaction.hash()))
        .createdAt(transaction.createdAt())
        .modifiedAt(transaction.modifiedAt())
        .sourceAddress(transaction.account() + transaction.sourceTag())
        .destinationAddress(transaction.destination() + transaction.destinationTag())
        .amount(transaction.amount())
        .denomination(transaction.denomination())
        .build();
      this.onPayment(payment);
    }
  }
}
