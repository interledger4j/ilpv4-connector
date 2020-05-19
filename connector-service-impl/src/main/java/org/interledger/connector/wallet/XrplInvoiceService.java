package org.interledger.connector.wallet;

import org.interledger.connector.opa.PaymentSystemFacade;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.openpayments.events.PaymentCompletedEvent;

import com.google.common.eventbus.EventBus;
import org.springframework.core.convert.ConversionService;

import java.util.function.Supplier;

public class XrplInvoiceService extends AbstractInvoiceService<XrpPayment, XrpPaymentDetails> implements XrplPaymentEventHandler {

  public XrplInvoiceService(
    InvoicesRepository invoicesRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsClient openPaymentsClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
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
  }

  @Override
  public void onPaymentCompleted(PaymentCompletedEvent paymentCompletedEvent) {

  }
}
