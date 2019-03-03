package org.interledger.connector.link.blast;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.collect.Lists;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Date;
import java.util.Objects;
import java.util.function.Supplier;

import static org.interledger.connector.link.blast.BlastHeaders.BLAST_AUDIENCE;
import static org.interledger.connector.link.blast.BlastHeaders.ILP_HEADER_OCTET_STREAM;
import static org.interledger.connector.link.blast.BlastHeaders.ILP_OCTET_STREAM;
import static org.interledger.connector.link.blast.BlastHeaders.ILP_OPERATOR_ADDRESS_VALUE;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.POST;

/**
 * Encapsulates how to communicate with a BLAST peer.
 */
public class BlastHttpSender {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  // Server Endpoint.
  private final InterledgerAddress operatorAddress;
  private final URI uri;
  private final RestTemplate restTemplate;
  private final Supplier<String> tokenIssuerSupplier;
  private final Supplier<String> accountIdSupplier;
  private final Supplier<byte[]> accountSecretSupplier;

  public BlastHttpSender(final InterledgerAddress operatorAddress, final URI uri, final RestTemplate restTemplate,
                         final Supplier<String> tokenIssuerSupplier,
                         final Supplier<String> accountIdSupplier,
                         final Supplier<byte[]> accountSecretSupplier) {
    this.operatorAddress = Objects.requireNonNull(operatorAddress);
    this.uri = Objects.requireNonNull(uri);
    this.restTemplate = Objects.requireNonNull(restTemplate);
    this.tokenIssuerSupplier = Objects.requireNonNull(tokenIssuerSupplier);
    this.accountIdSupplier = accountIdSupplier;
    this.accountSecretSupplier = Objects.requireNonNull(accountSecretSupplier);
  }

  /**
   * Send an ILP prepare packet to the remote peer.
   *
   * @param preparePacket
   *
   * @return An optionally-present {@link InterledgerResponsePacket}. If the request to the remote peer times-out, then
   * the ILP reject packet will contain a {@link InterledgerRejectPacket#getTriggeredBy()} address that   that matches
   * this node's operator address.
   */
  public InterledgerResponsePacket sendData(final InterledgerPreparePacket preparePacket) {
    final HttpHeaders headers = constructBlastRequestHeaders();
    final RequestEntity<InterledgerPreparePacket> requestEntity
      = new RequestEntity<>(preparePacket, headers, POST, uri);
    final ResponseEntity<InterledgerResponsePacket> response = restTemplate
      .exchange(requestEntity, InterledgerResponsePacket.class);

    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    } else if (response.getStatusCode().is4xxClientError()) {
      // Reject!
      return InterledgerRejectPacket.builder()
        .triggeredBy(operatorAddress)
        .code(InterledgerErrorCode.F00_BAD_REQUEST)
        .build();
    } else { //if (response.getStatusCode().is5xxServerError()) {
      // Reject per RFC "should" language...
      return InterledgerRejectPacket.builder()
        .triggeredBy(operatorAddress)
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .build();
    }
  }

  /**
   * <p>Check the `/ilp` endpoint for ping by making an HTTP Head request, and asserting the values returned are one of
   * the supported content-types required for BLAST.</p>
   *
   * <p>If the endpoint does not support producing BLAST responses, we expect a 406 NOT_ACCEPTABLE response. If the
   * endpoint does not support BLAST requests, then we expect a 415 UNSUPPORTED_MEDIA_TYPE.</p>
   */
  public void testConnection() {
    try {
      final HttpHeaders headers = constructBlastRequestHeaders();
      final RequestEntity<InterledgerPreparePacket> requestEntity = new RequestEntity<>(headers, HEAD, uri);
      final ResponseEntity<HttpHeaders> response = restTemplate.exchange(
        requestEntity, new ParameterizedTypeReference<HttpHeaders>() {
        }
      );

      if (response.getStatusCode().is2xxSuccessful()) {
        // If there's one Accept header we can work with, then treat this as a successful test connection...
        boolean hasSupportedHeader = response.getHeaders().getAccept().stream()
          .filter(
            acceptHeader -> acceptHeader.equals(ILP_HEADER_OCTET_STREAM) || acceptHeader.equals(ILP_OCTET_STREAM)
          )
          .findAny().isPresent();

        if (hasSupportedHeader) {
          logger.info("Remote peer `{}` supports BLAST: {}", uri, response.getHeaders());
        } else {
          throw new HttpClientErrorException(
            HttpStatus.BAD_REQUEST,
            String.format("Remote peer `%s` DOES NOT support BLAST: %s", uri, response.getHeaders())
          );
        }
      } else if (response.getStatusCode().is4xxClientError()) {
        if (response.getStatusCode().equals(HttpStatus.NOT_ACCEPTABLE)) {
          throw new HttpClientErrorException(
            response.getStatusCode(),
            String.format("Remote BLAST endpoint(`%s`) does not support producing BLAST responses.", uri)
          );
        } else if (response.getStatusCode().equals(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
          throw new HttpClientErrorException(
            response.getStatusCode(),
            String.format("Remote BLAST endpoint(`%s`) does not support incoming BLAST requests.", uri)
          );
        } else {
          throw new HttpClientErrorException(
            response.getStatusCode(),
            String.format("Unable to connect to remote BLAST endpoint(`%s`)", uri)
          );
        }
      }
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        logger.warn(String.format("Unable to connect to remote BLAST endpoint (`%s`): %s", uri, e.getMessage()));
      } else {
        throw new HttpClientErrorException(
          e.getStatusCode(),
          String.format("Unable to connect to remote BLAST peer(`%s`): %s", uri, e.getStatusText())
        );
      }
    } catch (ResourceAccessException e) { // Remote endpoint is not serving...
      logger.warn(String.format("Unable to connect to remote BLAST endpoint (`%s`): %s", uri, e.getMessage()));
    }
  }

  /**
   * Accessor for the encapsulated {@link RestTemplate}.
   */
  public RestTemplate getRestTemplate() {
    return restTemplate;
  }

  private HttpHeaders constructBlastRequestHeaders() {
    // TODO: Set proper caching headers to prevent caching.

    final HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Lists.newArrayList(ILP_OCTET_STREAM));
    headers.setContentType(ILP_OCTET_STREAM);
    headers.set(ILP_OPERATOR_ADDRESS_VALUE, operatorAddress.getValue());
    headers.setBearerAuth(this.constructAuthToken());
    return headers;
  }

  private String constructAuthToken() {
    return JWT.create()
      .withIssuedAt(new Date())
      .withIssuer(tokenIssuerSupplier.get())
      .withSubject(accountIdSupplier.get()) // account identifier at the remote server.
      .withAudience(BLAST_AUDIENCE)
      .sign(Algorithm.HMAC256(accountSecretSupplier.get()));
  }

}
