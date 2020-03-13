package org.interledger.connector.server.wallet.controllers;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.SPSP_ENABLED;
import static org.interledger.connector.core.ConfigConstants.SPSP__URL_PATH;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.problems.spsp.InvalidSpspRequestProblem;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.receiver.StreamReceiver;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UrlPathHelper;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

/**
 * Services HTTP GET requests in accordance with the SPSP4 protocol.
 *
 * @see "https://interledger.org/rfcs/0009-simple-payment-setup-protocol/"
 */
@RestController
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = SPSP_ENABLED, havingValue = TRUE)
public class SpspController {

  private final StreamReceiver streamReceiver;
  private final UrlPathHelper urlPathHelper;
  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final Optional<String> spspUrlPath;

  public SpspController(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final StreamReceiver streamReceiver,
    @Value("${" + SPSP__URL_PATH + "}") final String spspUrlPath
  ) {
    this.streamReceiver = Objects.requireNonNull(streamReceiver);
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.spspUrlPath = cleanupSpspUrlPath(spspUrlPath);
    this.urlPathHelper = new UrlPathHelper();
  }


  /**
   * A simple SPSP endpoint that merely returns a new Shared Secret and destination address to support a stateless
   * receiver.
   *
   * @return A {@link ResponseEntity} containing a {@link StreamConnectionDetails}.
   */
  @RequestMapping(
    path = "/**", method = RequestMethod.GET,
    produces = {"application/spsp4+json", APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ResponseEntity<StreamConnectionDetails> getSpspResponse(final HttpServletRequest httpServletRequest) {
    final String requestedUrlPath = urlPathHelper.getPathWithinApplication(httpServletRequest);
    if (StringUtils.isBlank(requestedUrlPath)) {
      throw new InvalidSpspRequestProblem();
    }
    // E.g., /p/foo.bar/baz will map to `g.connector.foo.bar.baz`
    final String ilpIntermediateSuffix = this.computePaymentTargetIntermediatePrefix(requestedUrlPath);
    if (ilpIntermediateSuffix.length() <= 0) {
      throw new InvalidSpspRequestProblem();
    }

    final InterledgerAddress paymentReceiverAddress = connectorSettingsSupplier.get().operatorAddress()
      .with(ilpIntermediateSuffix);

    final StreamConnectionDetails streamConnectionDetails = streamReceiver.setupStream(paymentReceiverAddress);

    // TODO: Validate that the content-type is `application/spsp+json`
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // TODO: Add client-cache directive per RFC (i.e., configurable max-age).
    return new ResponseEntity(streamConnectionDetails, headers, HttpStatus.OK);
  }

  /**
   * The URL prefix path for SPSP requests is configurable and can be null, or optionally include a leading or trailing
   * or forward-slash. This method cleanses the input so that whatever is supplied will work properly in this
   * Controller.
   *
   * @param spspUrlPath A {@link String} representing the configured SPSP URL prefix path.
   *
   * @return An optionally-present {@link String} containing the cleansed path.
   */
  @VisibleForTesting
  protected final Optional<String> cleanupSpspUrlPath(String spspUrlPath) {
    if (StringUtils.isBlank(spspUrlPath) || StringUtils.equals(spspUrlPath, "/")) {
      return Optional.empty();
    } else {
      if (!spspUrlPath.startsWith("/")) { // add leading
        spspUrlPath = "/" + spspUrlPath;
      }

      if (spspUrlPath.endsWith("/")) { // remove trailing
        spspUrlPath = spspUrlPath.substring(0, spspUrlPath.length() - 1);
      }

      // Any double-forward-slashes should be replaced with a single.
      spspUrlPath = spspUrlPath.replaceAll("//", "/");

      return Optional.ofNullable(spspUrlPath);
    }
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
   * @param requestUrlPath A {@link String} containing the request's URL path (e.g., `/p/foo.bar/baz`). processing an
   *                       SPSP request.
   *
   * @return
   */
  @VisibleForTesting
  protected final String computePaymentTargetIntermediatePrefix(final String requestUrlPath) {
    Objects.requireNonNull(requestUrlPath);

    // path will have the leading configured path stripped off.
    String paymentTarget = this.spspUrlPath
      .map(spspUrlPath -> {
        if (!StringUtils.startsWith(requestUrlPath, spspUrlPath)) {
          return "";
        } else {
          // Strip off the SPSP url path as configured.
          String returnable = StringUtils.trimToEmpty(requestUrlPath).replace(spspUrlPath, "");
          if (returnable.endsWith("/")) {
            returnable = returnable.substring(0, returnable.length() - 1); // remove trailing slash.
          }
          return returnable;
        }
      })
      .orElse(StringUtils.trimToEmpty(requestUrlPath));

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
}
