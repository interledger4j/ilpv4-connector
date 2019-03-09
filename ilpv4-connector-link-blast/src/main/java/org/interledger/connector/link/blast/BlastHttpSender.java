package org.interledger.connector.link.blast;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.collect.Lists;
import okhttp3.HttpUrl;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;

import static org.interledger.connector.link.PingableLink.PING_PROTOCOL_CONDITION;
import static org.interledger.connector.link.blast.BlastHeaders.BLAST_AUDIENCE;
import static org.interledger.connector.link.blast.BlastHeaders.ILP_HEADER_OCTET_STREAM;
import static org.interledger.connector.link.blast.BlastHeaders.ILP_OCTET_STREAM;
import static org.interledger.connector.link.blast.BlastHeaders.ILP_OPERATOR_ADDRESS_VALUE;
import static org.springframework.http.HttpMethod.POST;

/**
 * Encapsulates how to communicate with a BLAST peer.
 */
public class BlastHttpSender {

  private static final InterledgerPreparePacket UNFULFILLABLE_PACKET = InterledgerPreparePacket.builder()
    .executionCondition(PING_PROTOCOL_CONDITION)
    .expiresAt(Instant.now().plusSeconds(30))
    .destination(InterledgerAddress.of("peer.ping"))
    .build();

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // Sometimes the Operator Address is not yet populated. In this class, there are times when we require it to be
  // populated, and there are times when we expect this value to possibly be un-initialized.d
  private final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier;

  // Server Endpoint.
  private final URI uri;
  private final RestTemplate restTemplate;
  private final Supplier<HttpUrl> tokenIssuerSupplier;
  private final Supplier<String> accountIdSupplier;
  private final Supplier<byte[]> accountSecretSupplier;

  /**
   * @param operatorAddressSupplier A {@link Supplier} for the ILP address of the node operating this BLAST sender.
   * @param uri                     The URI of the HTTP endpoint to send BLAST requests to.
   * @param restTemplate            A {@link RestTemplate} to use to communicate with the remote BLAST endpoint.
   * @param tokenIssuerSupplier     A {@link Supplier} for accessing the Auth token to use when communicating with the
   *                                remote endpoint.
   * @param accountIdSupplier       A {@link Supplier} for the AccountId to use when communicating with the * remote
   *                                endpoint.
   * @param accountSecretSupplier   A {@link Supplier} for the Account shared-secret to use when communicating with the
   *                                remote endpoint.
   */
  public BlastHttpSender(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
    final URI uri,
    final RestTemplate restTemplate,
    final Supplier<HttpUrl> tokenIssuerSupplier,
    final Supplier<String> accountIdSupplier,
    final Supplier<byte[]> accountSecretSupplier
  ) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
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
        .triggeredBy(operatorAddressSupplier())
        .code(InterledgerErrorCode.F00_BAD_REQUEST)
        .build();
    } else { //if (response.getStatusCode().is5xxServerError()) {
      // Reject per RFC "should" language...
      return InterledgerRejectPacket.builder()
        .triggeredBy(operatorAddressSupplier())
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .build();
    }
  }

  /**
   * <p>Check the `/ilp` endpoint for ping by making an HTTP Head request with a ping packet, and
   * asserting the values returned are one of the supported content-types required for BLAST.</p>
   *
   * <p>If the endpoint does not support producing BLAST responses, we expect a 406 NOT_ACCEPTABLE response. If the
   * endpoint does not support BLAST requests, then we expect a 415 UNSUPPORTED_MEDIA_TYPE.</p>
   */
  public void testConnection() {
    try {

      final HttpHeaders headers = constructBlastRequestHeaders();
      final RequestEntity<InterledgerPreparePacket> requestEntity
        = new RequestEntity<>(UNFULFILLABLE_PACKET, headers, POST, uri);
      final ResponseEntity<InterledgerResponsePacket> response = restTemplate
        .exchange(requestEntity, InterledgerResponsePacket.class);
      if (response.getStatusCode().is2xxSuccessful()) {
        // If there's one Accept header we can work with, then treat this as a successful test connection...
        boolean hasSupportedHeader =
          response.getHeaders().getContentType().equals(ILP_HEADER_OCTET_STREAM) ||
            response.getHeaders().getContentType().equals(ILP_OCTET_STREAM);

        if (hasSupportedHeader) {
          logger.info("Remote peer `{}` supports BLAST!", uri);
          logger.debug("Remote peer `{}` supports BLAST: {}", uri, response.getHeaders());
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

    // Set the Operator Address header, if present.
    operatorAddressSupplier.get().ifPresent(operatorAddress -> {
      headers.set(ILP_OPERATOR_ADDRESS_VALUE, operatorAddress.getValue());
    });

    headers.setBearerAuth(this.constructAuthToken());
    return headers;
  }

  private String constructAuthToken() {
    return JWT.create()
      .withIssuedAt(new Date())
      .withIssuer(tokenIssuerSupplier.get().toString())
      .withSubject(accountIdSupplier.get()) // account identifier at the remote server.
      .withAudience(BLAST_AUDIENCE)
      .sign(Algorithm.HMAC256(accountSecretSupplier.get()));
  }

  /**
   * Helper-method to de-reference the operator address.
   */
  private InterledgerAddress operatorAddressSupplier() {
    return operatorAddressSupplier.get().get();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BlastHttpSender that = (BlastHttpSender) o;

    if (!operatorAddressSupplier.equals(that.operatorAddressSupplier)) {
      return false;
    }
    if (!uri.equals(that.uri)) {
      return false;
    }
    if (!restTemplate.equals(that.restTemplate)) {
      return false;
    }
    if (!tokenIssuerSupplier.equals(that.tokenIssuerSupplier)) {
      return false;
    }
    if (!accountIdSupplier.equals(that.accountIdSupplier)) {
      return false;
    }
    return accountSecretSupplier.equals(that.accountSecretSupplier);
  }

  @Override
  public int hashCode() {
    int result = operatorAddressSupplier.hashCode();
    result = 31 * result + uri.hashCode();
    result = 31 * result + restTemplate.hashCode();
    result = 31 * result + tokenIssuerSupplier.hashCode();
    result = 31 * result + accountIdSupplier.hashCode();
    result = 31 * result + accountSecretSupplier.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", BlastHttpSender.class.getSimpleName() + "[", "]")
      .add("operatorAddressSupplier=" + operatorAddressSupplier.get())
      .add("uri=" + uri)
      .add("restTemplate=" + restTemplate)
      .add("tokenIssuerSupplier=" + tokenIssuerSupplier.get())
      .add("accountIdSupplier=" + accountIdSupplier.get())
      .add("accountSecretSupplier=[********]")
      .toString();
  }
}
