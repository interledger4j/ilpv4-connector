package org.interledger.connector.opa.service.ilp;

import static org.interledger.connector.core.ConfigConstants.SPSP__URL_PATH;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.InvoiceId;
import org.interledger.connector.opa.model.problems.InvalidInvoiceSubjectProblem;
import org.interledger.connector.opa.service.InvoiceService;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;

import com.google.common.annotations.VisibleForTesting;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class IlpInvoiceService implements InvoiceService {

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final Optional<String> opaUrlPath;
  private final PaymentPointerResolver paymentPointerResolver;

  public IlpInvoiceService(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final PaymentPointerResolver paymentPointerResolver,
    final String opaUrlPath
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.opaUrlPath = cleanupOpaUrlPath(opaUrlPath);
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
    final String ilpIntermediateSuffix = this.computePaymentTargetIntermediatePrefix(paymentPointerPath);
    if (StringUtils.isBlank(ilpIntermediateSuffix)) {
      throw new InvalidInvoiceSubjectProblem();
    }

    return connectorSettingsSupplier.get().operatorAddress()
      .with(ilpIntermediateSuffix).getValue();
  }

  /**
   * Given a string representing a URL path, compute the intermediate Interledger address suffix according to the
   * following rules:
   *
   * <ol>
   *   <li>If there is a configured SPSP URL path, ignore that path segment in {@code requestUrlPath} and start
   *   with the next URL path segment. For example, if the SPSP UrlPath is configured to be `/p`, then `/p/marty`
   *   would yield `/marty`.</li>
   *   <li>Next, transpose all remaining paths into a dot-separated string by replacing any forward-slashes with
   *   periods.</li>
   *   <li>The resulting string is the ILP intermediate prefix.</li>
   * </ol>
   *
   * @param paymentPointerPath A {@link String} containing the request's URL path (e.g., `/p/foo.bar/baz`). processing an
   *                       SPSP request.
   *
   * TODO: Put this in a common place for SPSP and OPA
   * @return
   */
  protected final String computePaymentTargetIntermediatePrefix(final String paymentPointerPath) {
    Objects.requireNonNull(paymentPointerPath);

    // path will have the leading configured path stripped off.
    String paymentTarget = this.opaUrlPath
      .map(opaUrlPath -> {
        if (!StringUtils.startsWith(paymentPointerPath, opaUrlPath)) {
          return "";
        } else {
          // Strip off the SPSP url path as configured.
          String returnable = StringUtils.trimToEmpty(paymentPointerPath).replace(opaUrlPath, "");
          if (returnable.endsWith("/")) {
            returnable = returnable.substring(0, returnable.length() - 1); // remove trailing slash.
          }
          return returnable;
        }
      })
      .orElse(StringUtils.trimToEmpty(paymentPointerPath));

    paymentTarget = paymentTarget.replace("/", ".");
    paymentTarget = paymentTarget.replace("..", ".");

    // Sanitize any leading or trailing periods so that this value can safely be appended to an ILP address/prefix.
    if (StringUtils.startsWith(paymentTarget, ".")) {
      paymentTarget = paymentTarget.replaceFirst(".", "");
    }
    if (StringUtils.endsWith(paymentTarget, ".")) {
      paymentTarget = paymentTarget.substring(0, paymentTarget.length() - 1);
    }

    return paymentTarget;
  }

  /**
   * The URL prefix path for Open Payments OPTIONS requests is configurable and can be null, or optionally include a leading or trailing
   * forward-slash. This method cleanses the input so that whatever is supplied will work properly in this Controller.
   *
   * TODO: Put this in a common place for SPSP and OPA
   * @param opaUrlPath A {@link String} representing the configured SPSP URL prefix path.
   *
   * @return An optionally-present {@link String} containing the cleansed path.
   */
  @VisibleForTesting
  protected final Optional<String> cleanupOpaUrlPath(String opaUrlPath) {
    String cleanedUpOpaUrlPath = opaUrlPath;
    if (StringUtils.isBlank(cleanedUpOpaUrlPath) || StringUtils.equals(cleanedUpOpaUrlPath, "/")) {
      return Optional.empty();
    } else {
      if (!cleanedUpOpaUrlPath.startsWith("/")) { // add leading
        cleanedUpOpaUrlPath = "/" + cleanedUpOpaUrlPath;
      }

      if (cleanedUpOpaUrlPath.endsWith("/")) { // remove trailing
        cleanedUpOpaUrlPath = cleanedUpOpaUrlPath.substring(0, cleanedUpOpaUrlPath.length() - 1);
      }

      // Any double-forward-slashes should be replaced with a single.
      cleanedUpOpaUrlPath = cleanedUpOpaUrlPath.replaceAll("//", "/");

      return Optional.ofNullable(cleanedUpOpaUrlPath);
    }
  }
}
