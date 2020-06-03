package org.interledger.connector.wallet;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.sub.LocalDestinationAddressUtils;
import org.interledger.connector.opa.PaymentSystemFacade;
import org.interledger.connector.opa.model.IlpPaymentDetails;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.problems.InvalidInvoiceSubjectProblem;
import org.interledger.connector.payments.SendPaymentRequest;
import org.interledger.connector.payments.SendPaymentService;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.payments.StreamPaymentType;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

public class IlpPaymentSystemFacade implements PaymentSystemFacade<StreamPayment, IlpPaymentDetails> {

  private final Optional<String> opaUrlPath;
  private final PaymentPointerResolver paymentPointerResolver;
  private StreamConnectionGenerator streamConnectionGenerator;
  private ServerSecretSupplier serverSecretSupplier;
  private final SendPaymentService sendPaymentService;
  private LocalDestinationAddressUtils localDestinationAddressUtils;

  public IlpPaymentSystemFacade(
    String opaUrlPath,
    PaymentPointerResolver paymentPointerResolver,
    StreamConnectionGenerator streamConnectionGenerator,
    ServerSecretSupplier serverSecretSupplier,
    SendPaymentService sendPaymentService, LocalDestinationAddressUtils localDestinationAddressUtils
  ) {
    this.opaUrlPath = PaymentDetailsUtils.cleanupUrlPath(opaUrlPath);
    this.paymentPointerResolver = Objects.requireNonNull(paymentPointerResolver);
    this.streamConnectionGenerator = Objects.requireNonNull(streamConnectionGenerator);
    this.serverSecretSupplier = Objects.requireNonNull(serverSecretSupplier);
    this.sendPaymentService = sendPaymentService;
    this.localDestinationAddressUtils = localDestinationAddressUtils;
  }

  @Override
  public IlpPaymentDetails getPaymentDetails(Invoice invoice) {

    final String ilpIntermediateSuffix = this.validateInvoiceSubjectAndComputeAddressSuffix(invoice.subject());

    String[] suffixParts = ilpIntermediateSuffix.split("\\.");
    AccountId receiverAccountId = AccountId.of(suffixParts[suffixParts.length - 1]);
    final InterledgerAddress paymentReceiverAddress =
      localDestinationAddressUtils.getLocalFulfillmentAddress(receiverAccountId);

    // Get shared secret and address with connection tag
    final StreamConnectionDetails streamConnectionDetails =
      streamConnectionGenerator.generateConnectionDetails(serverSecretSupplier, paymentReceiverAddress);

    // TODO(bridge): Replace this with a bridge call instead of a SendPaymentService direct call.
    sendPaymentService.createPlaceholderPayment(receiverAccountId,
      StreamPaymentType.PAYMENT_RECEIVED,
      streamConnectionDetails.destinationAddress(),
      Optional.of(invoice.correlationId().value()),
      Optional.of(invoice.amount().minus(invoice.received()).bigIntegerValue())
    );

    return IlpPaymentDetails.builder()
      .destinationAddress(streamConnectionDetails.destinationAddress())
      .sharedSecret(streamConnectionDetails.sharedSecret())
      .build();
  }

  @Override
  public StreamPayment payInvoice(
    IlpPaymentDetails paymentDetails,
    AccountId senderAccountId,
    UnsignedLong amount,
    CorrelationId correlationId
  ) {
    // Send payment using STREAM
    // TODO(bridge): Let the bridge know that an invoice payment is being sent instead of this
    //                direct call?
    return sendPaymentService.sendMoney(
      SendPaymentRequest.builder()
      .accountId(senderAccountId)
      .amount(amount)
      .correlationId(correlationId.value())
      .streamConnectionDetails(
        StreamConnectionDetails.builder()
          .destinationAddress(paymentDetails.destinationAddress())
        .sharedSecret(paymentDetails.sharedSecret())
        .build())
      .build()
    );
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
