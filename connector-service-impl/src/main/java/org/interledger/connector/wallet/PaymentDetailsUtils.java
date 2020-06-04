package org.interledger.connector.wallet;

import org.interledger.core.InterledgerAddress;
import org.interledger.openpayments.Invoice;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

public class PaymentDetailsUtils {

  /**
   * Given a string representing a URL path, compute the intermediate Interledger address suffix according to the
   * following rules:
   *
   * <ol>
   *   <li>If there is a configured SPSP or Open Payments URL path, ignore that path segment in {@code requestUrlPath}
   *   and start with the next URL path segment. For example, if the Url Path is configured to be `/p`, then `/p/marty`
   *   would yield `/marty`.</li>
   *   <li>Next, transpose all remaining paths into a dot-separated string by replacing any forward-slashes with
   *   periods.</li>
   *   <li>The resulting string is the ILP intermediate prefix.</li>
   * </ol>
   *
   * @param prefixPath A {@link String} containing the path to compute an ILP Address Suffix from.
   *                   For SPSP requests, this will be the path of the SPSP request.
   *                   For Open Payments, this will be the path of the Payment Pointer of the subject of an {@link Invoice}.
   *
   * @return A {@link String} representing an {@link InterledgerAddress} which will be used as a suffix to another address.
   */
  public static final String computePaymentTargetIntermediatePrefix(
    final String prefixPath,
    final Optional<String> baseUrlPath
  ) {
    Objects.requireNonNull(prefixPath);

    // path will have the leading configured path stripped off.
    String paymentTarget = getPaymentTarget(prefixPath, baseUrlPath);

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

  protected static String getPaymentTarget(String prefixPath, Optional<String> baseUrlPath) {
    return baseUrlPath
      .map(urlPath -> {
        if (!StringUtils.startsWith(prefixPath, urlPath)) {
          return "";
        } else {
          // Strip off the url path as configured.
          String returnable = StringUtils.trimToEmpty(prefixPath).replace(urlPath, "");
          if (returnable.endsWith("/")) {
            returnable = returnable.substring(0, returnable.length() - 1); // remove trailing slash.
          }
          return returnable;
        }
      })
      .orElse(StringUtils.trimToEmpty(prefixPath));
  }

  /**
   * The URL prefix path for SPSP GET requests and Open Payments OPTIONS requests is configurable and can be null,
   * or optionally include a leading or trailing forward-slash.
   * This method cleanses the input so that whatever is supplied will work properly in this Controller.
   *
   * @param urlPath A {@link String} representing the configured SPSP or Open Payments URL prefix path.
   *
   * @return An optionally-present {@link String} containing the cleansed path.
   */
  @VisibleForTesting
  public static final Optional<String> cleanupUrlPath(String urlPath) {
    String cleanedUpUrlPath = urlPath;
    if (StringUtils.isBlank(cleanedUpUrlPath) || StringUtils.equals(cleanedUpUrlPath, "/")) {
      return Optional.empty();
    } else {
      if (!cleanedUpUrlPath.startsWith("/")) { // add leading
        cleanedUpUrlPath = "/" + cleanedUpUrlPath;
      }

      if (cleanedUpUrlPath.endsWith("/")) { // remove trailing
        cleanedUpUrlPath = cleanedUpUrlPath.substring(0, cleanedUpUrlPath.length() - 1);
      }

      // Any double-forward-slashes should be replaced with a single.
      cleanedUpUrlPath = cleanedUpUrlPath.replaceAll("//", "/");

      return Optional.ofNullable(cleanedUpUrlPath);
    }
  }
}
