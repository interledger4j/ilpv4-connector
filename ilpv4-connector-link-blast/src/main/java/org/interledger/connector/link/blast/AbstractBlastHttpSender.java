package org.interledger.connector.link.blast;

import com.google.common.collect.Lists;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static org.interledger.connector.link.blast.BlastHeaders.ILP_HEADER_OCTET_STREAM;
import static org.interledger.connector.link.blast.BlastHeaders.ILP_OCTET_STREAM;
import static org.interledger.connector.link.blast.BlastHeaders.ILP_OPERATOR_ADDRESS_VALUE;
import static org.springframework.http.HttpMethod.POST;

/**
 * An abstract implementation of {@link BlastHttpSender} for communicating with a remote ILP node using ILP-over-HTTP.
 */
public abstract class AbstractBlastHttpSender implements BlastHttpSender {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  // Sometimes the Operator Address is not yet populated. In this class, there are times when we require it to be
  // populated, and there are times when we expect this value to possibly be un-initialized.d
  private final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier;

  // Server Endpoint.
  private final URI uri;
  private final RestTemplate restTemplate;
  private final Supplier<String> accountIdSupplier;

  // Determined via testConnection when the Connection starts-up, if possible.
  private MediaType blastHeader = ILP_OCTET_STREAM;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A {@link Supplier} for the ILP address of the node operating this BLAST sender.
   * @param uri                     The URI of the HTTP endpoint to send BLAST requests to.
   * @param restTemplate            A {@link RestTemplate} to use to communicate with the remote BLAST endpoint.
   * @param accountIdSupplier       A {@link Supplier} for the AccountId to use when communicating with the remote
   *                                endpoint.
   */
  public AbstractBlastHttpSender(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
    final URI uri,
    final RestTemplate restTemplate,
    final Supplier<String> accountIdSupplier
  ) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
    this.uri = Objects.requireNonNull(uri);
    this.restTemplate = Objects.requireNonNull(restTemplate);
    this.accountIdSupplier = Objects.requireNonNull(accountIdSupplier);
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
      final RequestEntity<InterledgerPreparePacket> requestEntity = new RequestEntity<>(
        UNFULFILLABLE_PACKET, headers, POST, uri
      );
      final ResponseEntity<InterledgerResponsePacket> response = restTemplate.exchange(
        requestEntity, InterledgerResponsePacket.class
      );

      if (response.getStatusCode().is2xxSuccessful()) {
        // If there's one Accept header we can work with, then treat this as a successful test connection...
        boolean hasSupportedHeader = Optional.ofNullable(response.getHeaders().getContentType())
          .filter(contentType -> contentType.equals(ILP_HEADER_OCTET_STREAM) || contentType.equals(ILP_OCTET_STREAM) ||
            contentType.equals(MediaType.APPLICATION_OCTET_STREAM)
          ).map(ct -> true).orElse(false);

        if (hasSupportedHeader) {
          final MediaType contentType = response.getHeaders().getContentType();
          if (logger.isDebugEnabled()) {
            logger.debug("Remote peer-link `{}` ({}) supports ILP-over-HTTP (BLAST): {}",
              accountIdSupplier.get(), uri, response.getHeaders()
            );
          } else {
            logger.info("Remote peer `{}` ({}) supports ILP-over-HTTP (BLAST) using `{}`",
              accountIdSupplier.get(), uri, contentType
            );
          }
          this.blastHeader = Optional.ofNullable(response.getHeaders().getContentType()).orElse(ILP_OCTET_STREAM);
        } else {
          throw new HttpClientErrorException(
            HttpStatus.BAD_REQUEST,
            String.format("Remote peer-link `%s` (%s) DOES NOT support ILP-over-HTTP (BLAST)",
              accountIdSupplier.get(), uri
            ));
        }
      } else if (response.getStatusCode().is4xxClientError()) {
        if (response.getStatusCode().equals(HttpStatus.NOT_ACCEPTABLE)) {
          throw new HttpClientErrorException(
            response.getStatusCode(),
            String.format("Remote peer-link `%s` (%s) DOES NOT support ILP-over-HTTP (BLAST)",
              accountIdSupplier.get(), uri
            )
          );
        } else if (response.getStatusCode().equals(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
          throw new HttpClientErrorException(
            response.getStatusCode(),
            String.format("Remote peer-link `%s` (%s) DOES NOT support ILP-over-HTTP (BLAST)",
              accountIdSupplier.get(), uri
            )
          );
        } else {
          throw new HttpClientErrorException(
            response.getStatusCode(),
            String.format("Unable to connect to ILP-over-HTTP (BLAST) peer-link: `%s` (%s)",
              accountIdSupplier.get(), uri
            )
          );
        }
      }
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        logger.warn(String.format("Unable to connect to ILP-over-HTTP (BLAST) peer-link: `%s` (%s): %s",
          accountIdSupplier.get(), uri, e.getMessage())
        );
      } else {
        throw new HttpClientErrorException(
          e.getStatusCode(),
          String.format("Unable to connect to ILP-over-HTTP (BLAST) peer-link: `%s` (%s): %s",
            accountIdSupplier.get(), uri, e.getStatusText()
          )
        );
      }
    } catch (ResourceAccessException e) { // Remote endpoint is not serving...
      logger.warn(
        String.format("Unable to connect to ILP-over-HTTP (BLAST) peer-link: `%s` (%s): %s",
          accountIdSupplier.get(), uri, e.getMessage())
      );
    }
  }

  private HttpHeaders constructBlastRequestHeaders() {
    // TODO: Set proper caching headers to prevent caching.

    final HttpHeaders headers = new HttpHeaders();
    // Defaults to ILP_OCTET_STREAM, but is replaced by whatever testConnection returns if it's a valid media-type.
    headers.setAccept(Lists.newArrayList(blastHeader));
    headers.setContentType(blastHeader);

    // Set the Operator Address header, if present.
    operatorAddressSupplier.get().ifPresent(operatorAddress -> {
      headers.set(ILP_OPERATOR_ADDRESS_VALUE, operatorAddress.getValue());
    });

    headers.setBearerAuth(new String(this.constructAuthToken()));
    return headers;
  }

  protected abstract byte[] constructAuthToken();

  protected Supplier<Optional<InterledgerAddress>> getOperatorAddressSupplier() {
    return operatorAddressSupplier;
  }

  protected URI getUri() {
    return uri;
  }

  protected RestTemplate getRestTemplate() {
    return restTemplate;
  }

  protected Supplier<String> getAccountIdSupplier() {
    return accountIdSupplier;
  }

  /**
   * Helper-method to de-reference the operator address.
   */
  protected InterledgerAddress operatorAddressSupplier() {
    return operatorAddressSupplier.get().get();
  }

}
