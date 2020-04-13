package org.interledger.connector.server.wallet.controllers;

import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.SPSP_ENABLED;
import static org.interledger.connector.core.ConfigConstants.SPSP__URL_PATH;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.interledger.spsp.client.SpspClient.APPLICATION_SPSP4_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.problems.spsp.InvalidSpspRequestProblem;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.wallet.PaymentDetailsUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.receiver.StreamReceiver;

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

  private static final MediaType APPLICATION_SPSP4_JSON = MediaType.valueOf(APPLICATION_SPSP4_JSON_VALUE);

  private final StreamReceiver streamReceiver;
  private final UrlPathHelper urlPathHelper;
  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final Optional<String> spspUrlPath;

  public SpspController(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final StreamReceiver streamReceiver,
    @Value("${" + SPSP__URL_PATH + ":}") final String spspUrlPath
  ) {
    this.streamReceiver = Objects.requireNonNull(streamReceiver);
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.spspUrlPath = PaymentDetailsUtils.cleanupUrlPath(spspUrlPath);
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
    produces = {APPLICATION_SPSP4_JSON_VALUE, APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ResponseEntity<StreamConnectionDetails> getSpspResponse(final HttpServletRequest httpServletRequest) {
    final String requestedUrlPath = urlPathHelper.getPathWithinApplication(httpServletRequest);
    if (StringUtils.isBlank(requestedUrlPath)) {
      throw new InvalidSpspRequestProblem();
    }
    // E.g., /p/foo.bar/baz will map to `g.connector.foo.bar.baz`
    final String ilpIntermediateSuffix = PaymentDetailsUtils.computePaymentTargetIntermediatePrefix(
      requestedUrlPath,
      this.spspUrlPath
    );

    if (ilpIntermediateSuffix.length() <= 0) {
      throw new InvalidSpspRequestProblem();
    }

    final InterledgerAddress paymentReceiverAddress = connectorSettingsSupplier.get().operatorAddress()
      .with(connectorSettingsSupplier.get().spspSettings().addressPrefixSegment())
      .with(ilpIntermediateSuffix);

    final StreamConnectionDetails streamConnectionDetails = streamReceiver.setupStream(paymentReceiverAddress);

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_SPSP4_JSON);

    // TODO: Add client-cache directive per RFC (i.e., configurable max-age).
    return new ResponseEntity(streamConnectionDetails, headers, HttpStatus.OK);
  }
}
