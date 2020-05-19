package org.interledger.connector.wallet;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.OpenPaymentsPaymentService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PayInvoiceRequest;
import org.interledger.connector.opa.model.PaymentDetails;
import org.interledger.connector.opa.model.PaymentNetwork;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.openpayments.events.PaymentCompletedEvent;
import org.interledger.stream.SendMoneyResult;

import com.google.common.eventbus.EventBus;
import okhttp3.HttpUrl;
import org.springframework.core.convert.ConversionService;

import java.util.Optional;
import java.util.function.Supplier;

public class XrplInvoiceService extends AbstractInvoiceService<XrpPayment, XrpPaymentDetails> implements XrplPaymentEventHandler {
  private final XrpOpenPaymentsPaymentService xrpOpenPaymentsPaymentService;

  public XrplInvoiceService(
    InvoicesRepository invoicesRepository,
    ConversionService conversionService,
    InvoiceFactory invoiceFactory,
    OpenPaymentsClient openPaymentsClient,
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    XrpOpenPaymentsPaymentService xrpOpenPaymentsPaymentService,
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

    this.xrpOpenPaymentsPaymentService = xrpOpenPaymentsPaymentService;
  }

  @Override
  public XrpPaymentDetails getPaymentDetails(InvoiceId invoiceId) {
    final Invoice invoice = this.getInvoiceById(invoiceId);

    final HttpUrl invoiceUrl = invoice.invoiceUrl()
      .orElseThrow(() -> new IllegalStateException("Invoice should have a location after creation."));

    if (!isForThisWallet(invoiceUrl)) {
      return openPaymentsClient.getXrpInvoicePaymentDetails(invoiceUrl.uri());
    }

    return xrpOpenPaymentsPaymentService.getPaymentDetails(invoice);
  }

  @Override
  public XrpPayment payInvoice(InvoiceId invoiceId, AccountId senderAccountId, Optional<PayInvoiceRequest> payInvoiceRequest) {
    return null;
  }

  @Override
  public void onPaymentCompleted(PaymentCompletedEvent paymentCompletedEvent) {

  }
}
