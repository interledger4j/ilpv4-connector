package org.interledger.connector.wallet;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.opa.OpenPaymentsPaymentService;
import org.interledger.connector.opa.model.IlpPaymentDetails;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PaymentDetails;
import org.interledger.connector.opa.model.PaymentResponse;
import org.interledger.connector.opa.model.problems.InvalidInvoiceSubjectProblem;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.Denomination;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class IlpOpenPaymentsPaymentService implements OpenPaymentsPaymentService {

  private final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;
  private final Optional<String> opaUrlPath;
  private final PaymentPointerResolver paymentPointerResolver;
  private StreamConnectionGenerator streamConnectionGenerator;
  private ServerSecretSupplier serverSecretSupplier;

  public IlpOpenPaymentsPaymentService(
    Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier,
    String opaUrlPath,
    PaymentPointerResolver paymentPointerResolver,
    StreamConnectionGenerator streamConnectionGenerator,
    ServerSecretSupplier serverSecretSupplier
  ) {
    this.openPaymentsSettingsSupplier = Objects.requireNonNull(openPaymentsSettingsSupplier);
    this.opaUrlPath = PaymentDetailsUtils.cleanupUrlPath(opaUrlPath);
    this.paymentPointerResolver = Objects.requireNonNull(paymentPointerResolver);
    this.streamConnectionGenerator = Objects.requireNonNull(streamConnectionGenerator);
    this.serverSecretSupplier = Objects.requireNonNull(serverSecretSupplier);
  }

  @Override
  public PaymentDetails getPaymentDetails(Invoice invoice) {

    final String ilpIntermediateSuffix = this.validateInvoiceSubjectAndComputeAddressSuffix(invoice.subject());

    String destinationAddress = openPaymentsSettingsSupplier.get().ilpOperatorAddress()
      .with(ilpIntermediateSuffix).getValue();

    // Get shared secret and address with connection tag
    final StreamConnectionDetails streamConnectionDetails =
      streamConnectionGenerator.generateConnectionDetails(serverSecretSupplier, InterledgerAddress.of(destinationAddress));

    InterledgerAddress finalDestinationAddress = InterledgerAddress.of(streamConnectionDetails.destinationAddress().getValue() + "~" + invoice.paymentId());
    return IlpPaymentDetails.builder()
      .destinationAddress(finalDestinationAddress)
      .sharedSecret(streamConnectionDetails.sharedSecret())
      .build();
  }

  @Override
  public PaymentResponse payInvoice(
    PaymentDetails paymentDetails,
    AccountId senderAccountId,
    UnsignedLong amount,
    String bearerToken
  ) {
    // TODO: create a STREAM sender here or use neil's API?
    return null;
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
