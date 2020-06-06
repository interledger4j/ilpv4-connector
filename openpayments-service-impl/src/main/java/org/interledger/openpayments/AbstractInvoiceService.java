package org.interledger.openpayments;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.InvoiceEntity;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.connector.persistence.repositories.PaymentsRepository;
import org.interledger.openpayments.config.OpenPaymentsPathConstants;
import org.interledger.openpayments.config.OpenPaymentsSettings;
import org.interledger.openpayments.problems.InvoiceAlreadyExistsProblem;
import org.interledger.openpayments.problems.InvoiceNotFoundProblem;
import org.interledger.openpayments.problems.UnsupportedInvoiceOperationProblem;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import feign.FeignException;
import okhttp3.HttpUrl;
import org.interleger.openpayments.InvoiceService;
import org.interleger.openpayments.client.OpenPaymentsProxyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * An abstract implementation of {@link InvoiceService} which performs common operations on {@link Invoice}s.
 *
 * @param <PaymentResultType> The type of the result of a call to {@link InvoiceService#payInvoice}.
 * @param <PaymentDetailsType> The type of the payment details returned from a call to {@link InvoiceService#getPaymentDetails}
 */
public abstract class AbstractInvoiceService<PaymentResultType, PaymentDetailsType> implements InvoiceService<PaymentResultType, PaymentDetailsType> {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected final InvoicesRepository invoicesRepository;
  protected final PaymentsRepository paymentsRepository;
  protected final ConversionService conversionService;
  protected final InvoiceFactory invoiceFactory;
  protected final OpenPaymentsProxyClient openPaymentsProxyClient;
  protected final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;

  public AbstractInvoiceService(
    final InvoicesRepository invoicesRepository,
    final PaymentsRepository paymentsRepository,
    final ConversionService conversionService,
    final InvoiceFactory invoiceFactory,
    final OpenPaymentsProxyClient openPaymentsProxyClient,
    final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    final EventBus eventBus
  ) {
    this.invoicesRepository = Objects.requireNonNull(invoicesRepository);
    this.paymentsRepository = Objects.requireNonNull(paymentsRepository);
    this.conversionService = Objects.requireNonNull(conversionService);
    this.invoiceFactory = Objects.requireNonNull(invoiceFactory);
    this.openPaymentsProxyClient = Objects.requireNonNull(openPaymentsProxyClient);
    this.openPaymentsSettingsSupplier = Objects.requireNonNull(openPaymentsSettingsSupplier);
    eventBus.register(this);
  }

  @Override
  public Invoice createInvoice(final NewInvoice newInvoice, final AccountId accountId) {
    Objects.requireNonNull(newInvoice);
    Objects.requireNonNull(accountId);

    // Really only to determine if we need to proxy
    HttpUrl ownerAccountUrl = newInvoice.ownerAccountUrl();

    if (!isForThisWallet(ownerAccountUrl)) {
      HttpUrl createUrl = ownerAccountUrl.newBuilder()
        .addPathSegment(OpenPaymentsPathConstants.INVOICES)
        .build();
      return openPaymentsProxyClient.createInvoice(createUrl.uri(), newInvoice);
    }

    Invoice invoice = invoiceFactory.construct(newInvoice);

    return invoicesRepository.saveInvoice(invoice);
  }

  @Override
  public Optional<Invoice> findInvoiceByUrl(HttpUrl receiverInvoiceUrl, AccountId accountId) {
    return invoicesRepository.findAllInvoicesByReceiverInvoiceUrl(receiverInvoiceUrl)
      .stream()
      .filter(invoice -> invoice.accountId().equals(accountId))
      .findFirst();
  }

  @Override
  public Invoice syncInvoice(final HttpUrl receiverInvoiceUrl, final AccountId accountId) {
    Objects.requireNonNull(receiverInvoiceUrl);
    Objects.requireNonNull(accountId);

    // See if we have that invoice already
    List<Invoice> existingInvoices = invoicesRepository.findAllInvoicesByReceiverInvoiceUrl(receiverInvoiceUrl);
    existingInvoices
      .stream()
      .filter(invoice -> invoice.accountId().equals(accountId))
      .findFirst()
      .ifPresent(invoice -> {
        throw new InvoiceAlreadyExistsProblem(invoice.id());
      });

    // If not, get it from the receiver, or just copy it locally
    Invoice invoiceOfReceiver;
    if (!isForThisWallet(receiverInvoiceUrl)) {
      invoiceOfReceiver = this.getRemoteInvoice(receiverInvoiceUrl);
    } else {
      invoiceOfReceiver = existingInvoices
        .stream()
        .filter(invoice -> !invoice.accountId().equals(accountId))
        .findFirst()
        .orElseThrow(() -> new InvoiceNotFoundProblem(receiverInvoiceUrl));
    }

    // And set the accountId of the invoice to the sender's accountId
    Invoice invoiceWithCorrectAccountId = Invoice.builder()
      .from(invoiceOfReceiver)
      .id(InvoiceId.of(UUID.randomUUID().toString()))
      .accountId(accountId)
      .build();

    return invoicesRepository.saveInvoice(invoiceWithCorrectAccountId);

  }

  @Override
  public Invoice getInvoice(final InvoiceId invoiceId, final AccountId accountId) {
    Objects.requireNonNull(invoiceId);
    Objects.requireNonNull(accountId);

    return invoicesRepository.findInvoiceByInvoiceIdAndAccountId(invoiceId, accountId)
      .orElseThrow(() -> new InvoiceNotFoundProblem(invoiceId));
  }

  private Invoice getRemoteInvoice(final HttpUrl invoiceUrl) {
    try {
      return openPaymentsProxyClient.getInvoice(invoiceUrl.uri());
    } catch (FeignException e) {
      if (e.status() == 404) {
        throw new InvoiceNotFoundProblem("Original invoice was not found in invoice owner's records.", invoiceUrl);
      }

      throw e;
    }
  }

  @Override
  public Invoice updateInvoice(final Invoice invoice, final AccountId accountId) {
    Objects.requireNonNull(invoice);
    Objects.requireNonNull(accountId);

    return invoicesRepository.findByInvoiceIdAndAccountId(invoice.id(), accountId)
      .map(entity -> {
        entity.setReceived(invoice.received().longValue());
        InvoiceEntity saved = invoicesRepository.save(entity);
        return saved;
      })
      .map(entity -> this.conversionService.convert(entity, Invoice.class))
      .orElseThrow(() -> new InvoiceNotFoundProblem(invoice.id()));
  }

  @Override
  public PaymentDetailsType getPaymentDetails(InvoiceId invoiceId, AccountId accountId) {
    throw new UnsupportedInvoiceOperationProblem(invoiceId);
  }

  @Override
  public PaymentResultType payInvoice(InvoiceId invoiceId, AccountId senderAccountId, Optional<PayInvoiceRequest> payInvoiceRequest) throws UserAuthorizationRequiredException {
    throw new UnsupportedInvoiceOperationProblem(invoiceId);
  }

  /**
   * Update all of the {@link Invoice}s associated with a {@link Payment}, which has been detected on an underlying
   * payment rail.
   *
   * @param payment The {@link Payment} that was detected on an underlying payment rail.
   */
  protected void onPayment(final Payment payment) {
    Objects.requireNonNull(payment);

    List<Invoice> invoices = invoicesRepository.findAllInvoicesByCorrelationId(payment.correlationId());

    if (invoices.isEmpty()) {
      throw new IllegalArgumentException("Could not find invoice by correlation ID."); // FIXME: throw InvoiceNotFoundProblem
    }

    invoices
      .forEach(invoice -> {
        // See if we have already processed this payment for this invoice
        Optional<Payment> existingPayment = paymentsRepository.findPaymentByPaymentIdAndInvoiceId(payment.paymentId(), invoice.id());

        // If we haven't, do it
        if (!existingPayment.isPresent()) {
          // FIXME: If the invoice assetCode and assetScale are different than the account's assetCode and assetScale,
          //  we need to do a conversion.
          Invoice updatedInvoice = Invoice.builder()
            .from(invoice)
            .received(invoice.received().plus(payment.amount()))
            .build();
          this.updateInvoice(updatedInvoice, invoice.accountId());

          Payment paymentToSave = Payment.builder()
            .from(payment)
            .invoiceId(invoice.id())
            .build();
          paymentsRepository.savePayment(paymentToSave);
        }
      });
  }

  /**
   * Utility method to determine if an {@link Invoice}'s owner exists on this OPS.
   *
   * @param invoiceUrl The {@link HttpUrl} of the {@link Invoice}.
   * @return true if the {@link Invoice} is for this wallet, false if not.
   */
  protected boolean isForThisWallet(HttpUrl invoiceUrl) {
    HttpUrl issuer = openPaymentsSettingsSupplier.get().metadata().issuer();
    return issuer.host().equals(invoiceUrl.host()) && issuer.port() == invoiceUrl.port();
  }

  /**
   * Utility method to get the minimum of two {@link UnsignedLong}s, because {@link Math#min} does not allow
   * {@link UnsignedLong} and large {@link UnsignedLong}s can cause overflow.
   *
   * @param first One {@link UnsignedLong}.
   * @param second The other {@link UnsignedLong}.
   * @return The minimum of the two arguments.
   */
  protected UnsignedLong min(UnsignedLong first, UnsignedLong second) {
    if (first.compareTo(second) < 0) {
      return first;
    }

    return second;
  }
}
