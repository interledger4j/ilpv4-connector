package org.interledger.connector.payments.openpayments;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.PaymentId;
import org.interledger.connector.payments.OpenPaymentsEventHandler;
import org.interledger.connector.payments.OpenPaymentsFacade;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.payments.StreamPaymentType;
import org.interledger.core.InterledgerAddress;
import org.interledger.openpayments.Denomination;
import org.interledger.openpayments.Payment;
import org.interledger.openpayments.PaymentType;
import org.interledger.openpayments.events.PaymentCompletedEvent;

import com.google.common.eventbus.EventBus;
import org.interleger.openpayments.PaymentSystemEventHandler;
import org.interleger.openpayments.PaymentSystemFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * An in-memory bridge between the OpenPayments server and the Connector that uses an async {@link EventBus} so that the
 * packet-switch doesn't have to wait for the bridge to complete.
 */
public class InMemoryOpenPaymentsBridge
  implements OpenPaymentsEventHandler, PaymentSystemEventHandler, OpenPaymentsFacade, PaymentSystemFacade {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // The OpenPayments server's view of the Connector system
  private final OpenPaymentsEventHandler openPaymentsEventHandlerDelegate;
  private final PaymentSystemEventHandler paymentSystemEventHandlerDelegate;

  /**
   * Required-args Constructor.
   *
   * @param openPaymentsEventHandlerDelegate  A {@link OpenPaymentsEventHandler} to delegate to.
   * @param paymentSystemEventHandlerDelegate A {@link PaymentSystemEventHandler} to delegate to
  ¬ */
  public InMemoryOpenPaymentsBridge(
    final OpenPaymentsEventHandler openPaymentsEventHandlerDelegate,
    final PaymentSystemEventHandler paymentSystemEventHandlerDelegate
  ) {
    this.openPaymentsEventHandlerDelegate = Objects.requireNonNull(openPaymentsEventHandlerDelegate);
    this.paymentSystemEventHandlerDelegate = Objects.requireNonNull(paymentSystemEventHandlerDelegate);
  }

  //////////////////
  // Connector Layer
  //////////////////

  // Proxy the InvoiceCreatedEvent into the Connector
  @Override
  public void onInvoiceCreated(final org.interledger.connector.events.InvoiceCreateEvent invoiceCreateEvent) {
    Objects.requireNonNull(invoiceCreateEvent);

    // Call the openPaymentsEventHandlerDelegate directly. This delegate is implemented by the Connector layer.
    logger.debug("Delegating InvoiceCreateEvent to openPaymentsEventHandlerDelegate: {}", invoiceCreateEvent);
    this.openPaymentsEventHandlerDelegate.onInvoiceCreated(invoiceCreateEvent);
  }

  // Tell the OpenPayments server that a payment was completed.
  @Override
  public void emitPaymentCompleted(final StreamPayment streamPayment) {
    Objects.requireNonNull(streamPayment);

    final PaymentCompletedEvent paymentCompleteEventForOpa = PaymentCompletedEvent
      .builder()
      .payment(Payment.builder()
        .paymentId(PaymentId.of(streamPayment.streamPaymentId()))
        .type(toPaymentType(streamPayment.type()))
        .createdAt(streamPayment.createdAt())
        .modifiedAt(streamPayment.modifiedAt())
        .sourceAddress(streamPayment.sourceAddress().map(InterledgerAddress::getValue).orElse("unknown"))
        .amount(streamPayment.amount())
        .destinationAddress(streamPayment.destinationAddress().getValue())
        .denomination(Denomination.builder()
          .assetCode(streamPayment.assetCode())
          .assetScale(streamPayment.assetScale())
          .build())
        .build())
      .build();

    // Call the paymentSystemEventHandlerOpaProxy directly. This proxy allows the Connector to call into the
    // OpenPayments implementation.
    logger.debug("Emitting PaymentCompletedEvent to paymentSystemEventHandlerDelegate: {}", paymentCompleteEventForOpa);
    this.paymentSystemEventHandlerDelegate.onPaymentCompleted(paymentCompleteEventForOpa);
  }

  private PaymentType toPaymentType(final StreamPaymentType streamPaymentType) {
    Objects.requireNonNull(streamPaymentType);
    return PaymentType.valueOf(streamPaymentType.getAdjustmentType().name());
  }

  /////////////////////
  // OpenPayments Layer
  /////////////////////

  // Tell the PaymentSystem that an Invoice was created.
  @Override
  public void emitInvoiceCreated(final Invoice invoice) {
    Objects.requireNonNull(invoice);

    final org.interledger.connector.events.InvoiceCreateEvent invoiceCreateEventForConnector
      = org.interledger.connector.events.InvoiceCreateEvent.builder()
      .invoice(invoice)
      .build();

    // Call the paymentSystemEventHandlerDelegate directly. This delegate is implemented by the OpenPayments layer.
    logger.debug("Emitting InvoiceCreateEvent to paymentSystemEventHandlerDelegate: {}", invoiceCreateEventForConnector);
    this.openPaymentsEventHandlerDelegate.onInvoiceCreated(invoiceCreateEventForConnector);
  }

  // Proxy the paymentCompletedEvent into the OpenPayments layer.
  @Override
  public void onPaymentCompleted(final PaymentCompletedEvent paymentCompletedEvent) {
    Objects.requireNonNull(paymentCompletedEvent);
    // Delegate this call from this bridge into the delegate.
    logger.debug("Delegating PaymentCompletedEvent to paymentSystemFacadeDelegate: {}", paymentCompletedEvent);
    paymentSystemEventHandlerDelegate.onPaymentCompleted(paymentCompletedEvent);
  }
}
