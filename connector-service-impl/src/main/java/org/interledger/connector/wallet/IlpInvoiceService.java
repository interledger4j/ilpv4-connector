package org.interledger.connector.wallet;

import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.problems.InvalidInvoiceSubjectProblem;
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

  public IlpInvoiceService(
    final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    final PaymentPointerResolver paymentPointerResolver,
    final String opaUrlPath
  ) {
    this.openPaymentsSettingsSupplier = Objects.requireNonNull(openPaymentsSettingsSupplier);
    this.opaUrlPath = PaymentDetailsUtils.cleanupUrlPath(opaUrlPath);
    this.paymentPointerResolver = Objects.requireNonNull(paymentPointerResolver);
  }

  @Override
  public Invoice getInvoiceById(InvoiceId invoiceId) {
    return null;
  }

  @Override
  public Invoice createInvoice(Invoice invoice) {
    return null;
  }

  @Override
  public Invoice updateInvoice(Invoice invoice) {
    return null;
  }

  @Override
  public String getAddressFromInvoiceSubject(String subject) {

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
      throw new InvalidInvoiceSubjectProblem(subject);
    }

    return openPaymentsSettingsSupplier.get().ilpOperatorAddress()
      .with(ilpIntermediateSuffix).getValue();
  }
}
