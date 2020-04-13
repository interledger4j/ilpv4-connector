package org.interledger.connector.wallet;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.problems.InvalidInvoiceSubjectProblem;
import org.interledger.connector.opa.model.problems.InvoiceNotFoundProblem;
import org.interledger.connector.persistence.repositories.InvoicesRepository;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;

import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class IlpInvoiceService implements InvoiceService {

  private final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;
  private final Optional<String> opaUrlPath;
  private final PaymentPointerResolver paymentPointerResolver;
  private final InvoicesRepository invoicesRepository;

  public IlpInvoiceService(
    final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    final PaymentPointerResolver paymentPointerResolver,
    final String opaUrlPath,
    final InvoicesRepository invoicesRepository
    ) {
    this.openPaymentsSettingsSupplier = Objects.requireNonNull(openPaymentsSettingsSupplier);
    this.opaUrlPath = PaymentDetailsUtils.cleanupUrlPath(opaUrlPath);
    this.paymentPointerResolver = Objects.requireNonNull(paymentPointerResolver);
    this.invoicesRepository = Objects.requireNonNull(invoicesRepository);
  }

  @Override
  public Invoice getInvoiceById(InvoiceId invoiceId) {
    return invoicesRepository.findInvoiceByInvoiceId(invoiceId)
      .orElseThrow(() -> new InvoiceNotFoundProblem(invoiceId));
  }

  @Override
  public Invoice createInvoice(Invoice invoice) {
    validateInvoiceSubjectAndComputeAddressSuffix(invoice.subject());
    return invoicesRepository.saveInvoice(invoice);
  }

  @Override
  public Invoice updateInvoice(Invoice invoice) {
    return null;
  }

  @Override
  public String getAddressFromInvoiceSubject(String subject) {

    final String ilpIntermediateSuffix = this.validateInvoiceSubjectAndComputeAddressSuffix(subject);

    return openPaymentsSettingsSupplier.get().ilpOperatorAddress()
      .with(ilpIntermediateSuffix).getValue();
  }

  /**
   * Performs validation logic on the subject of an {@link Invoice} and computes an ILP address suffix from the subject.
   *
   * Ideally validation logic would be performed in the {@link Invoice} class.  However, the subject of an {@link Invoice}
   * is typed as a {@link String} and can take different forms, including a {@link PaymentPointer} or an XRP Address.
   *
   * @param subject The subject of an {@link Invoice}
   * @return A {@link String} representing an {@link InterledgerAddress} which will be used as a suffix to another address.
   * @throws InvalidInvoiceSubjectProblem if the {@code subject} is an invalid {@link PaymentPointer}
   *          or the subject did not contain a path to derive an address suffix from.
   */
  private String validateInvoiceSubjectAndComputeAddressSuffix(String subject) {
    try {
      PaymentPointer subjectPaymentPointer = PaymentPointer.of(subject);
      HttpUrl resolvedPaymentPointer = paymentPointerResolver.resolveHttpUrl(subjectPaymentPointer);

      String paymentPointerPath = resolvedPaymentPointer.pathSegments()
        .stream()
        .reduce("", (s, s2) -> s + "/" + s2);
      final String ilpIntermediateSuffix = PaymentDetailsUtils.computePaymentTargetIntermediatePrefix(
        paymentPointerPath,
        this.opaUrlPath
      );

      if (StringUtils.isBlank(ilpIntermediateSuffix)) {
        throw new InvalidInvoiceSubjectProblem("Invoice subject did not include user identifying information.", subject);
      }
      return ilpIntermediateSuffix;
    } catch (IllegalArgumentException e) {
      throw new InvalidInvoiceSubjectProblem("Invoice subject was an invalid Payment Pointer.", subject);
    }
  }
}
