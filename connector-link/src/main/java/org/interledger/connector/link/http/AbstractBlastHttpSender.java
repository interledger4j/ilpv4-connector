package org.interledger.connector.link.http;

import static org.interledger.connector.link.http.BlastHeaders.APPLICATION_ILP_HEADER_OCTET_STREAM;
import static org.interledger.connector.link.http.BlastHeaders.APPLICATON_ILP_OCTET_STREAM;
import static org.interledger.connector.link.http.BlastHeaders.ILP_OPERATOR_ADDRESS_VALUE;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An abstract implementation of {@link BlastHttpSender} for communicating with a remote ILP node using ILP-over-HTTP.
 */
@Deprecated
// TODO: Delete this. IlpLink is the replacement.
public abstract class AbstractBlastHttpSender implements BlastHttpSender {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  // Sometimes the Operator Address is not yet populated. In this class, there are times when we require it to be
  // populated, and there are times when we expect this value to possibly be un-initialized.d
  private final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier;
  private final RestTemplate restTemplate;
  private final OutgoingLinkSettings outgoingLinkSettings;

  // Determined via testConnection when the Connection starts-up, if possible.
  private MediaType blastHeader = APPLICATON_ILP_OCTET_STREAM;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A {@link Supplier} of this sender's operator {@link InterledgerAddress}.
   * @param restTemplate            A {@link RestTemplate} to use to communicate with the remote BLAST endpoint.
   * @param outgoingLinkSettings    A {@link OutgoingLinkSettings} for communicating with the remote endpoint.
   */
  public AbstractBlastHttpSender(
      final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
      final RestTemplate restTemplate,
      final OutgoingLinkSettings outgoingLinkSettings
  ) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
    this.restTemplate = Objects.requireNonNull(restTemplate);
    this.outgoingLinkSettings = Objects.requireNonNull(outgoingLinkSettings);
  }

  /**
   * Send an ILP prepare packet to the remote peer.
   *
   * @param preparePacket
   *
   * @return An optionally-present {@link InterledgerResponsePacket}. If the request to the remote peer times-out, then
   *     the ILP reject packet will contain a {@link InterledgerRejectPacket#getTriggeredBy()} address that   that
   *     matches this node's operator address.
   */
  public InterledgerResponsePacket sendData(final InterledgerPreparePacket preparePacket) {
    final HttpHeaders headers = constructBlastRequestHeaders();
    final RequestEntity<InterledgerPreparePacket> requestEntity =
        new RequestEntity<>(preparePacket, headers, HttpMethod.POST, outgoingLinkSettings.url().uri());
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
    final String accountId = outgoingLinkSettings.tokenSubject();
    try {
      final HttpHeaders headers = constructBlastRequestHeaders();
      final RequestEntity<InterledgerPreparePacket> requestEntity = new RequestEntity<>(
          UNFULFILLABLE_PACKET, headers, HttpMethod.POST, outgoingLinkSettings.url().uri()
      );
      final ResponseEntity<InterledgerResponsePacket> response = restTemplate.exchange(
          requestEntity, InterledgerResponsePacket.class
      );

      if (response.getStatusCode().is2xxSuccessful()) {
        // If there's one Accept header we can work with, then treat this as a successful test connection...
        boolean hasSupportedHeader = Optional.ofNullable(response.getHeaders().getContentType())
            .filter(contentType -> contentType.equals(APPLICATION_ILP_HEADER_OCTET_STREAM) || contentType.equals(
                APPLICATON_ILP_OCTET_STREAM) ||
                contentType.equals(MediaType.APPLICATION_OCTET_STREAM)
            ).map(ct -> true).orElse(false);

        if (hasSupportedHeader) {
          final MediaType contentType = response.getHeaders().getContentType();
          if (logger.isDebugEnabled()) {
            logger.debug("Remote peer-link `{}` ({}) supports ILP-over-HTTP (BLAST): {}",
                outgoingLinkSettings.tokenSubject(), outgoingLinkSettings.url(), response.getHeaders()
            );
          } else {
            logger.info("Remote peer `{}` ({}) supports ILP-over-HTTP (BLAST) using `{}`",
                accountId, outgoingLinkSettings.url(), contentType
            );
          }
          this.blastHeader = Optional.ofNullable(response.getHeaders().getContentType()).orElse(
              APPLICATON_ILP_OCTET_STREAM);
        } else {
          throw new HttpClientErrorException(
              HttpStatus.BAD_REQUEST,
              String.format("Remote peer-link `%s` (%s) DOES NOT support ILP-over-HTTP (BLAST)",
                  accountId, outgoingLinkSettings.url()
              ));
        }
      } else if (response.getStatusCode().is4xxClientError()) {
        if (response.getStatusCode().equals(HttpStatus.NOT_ACCEPTABLE)) {
          throw new HttpClientErrorException(
              response.getStatusCode(),
              String.format("Remote peer-link `%s` (%s) DOES NOT support ILP-over-HTTP (BLAST)",
                  accountId, outgoingLinkSettings.url()
              )
          );
        } else if (response.getStatusCode().equals(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
          throw new HttpClientErrorException(
              response.getStatusCode(),
              String.format("Remote peer-link `%s` (%s) DOES NOT support ILP-over-HTTP (BLAST)",
                  accountId, outgoingLinkSettings.url()
              )
          );
        } else {
          throw new HttpClientErrorException(
              response.getStatusCode(),
              String.format("Unable to connect to ILP-over-HTTP (BLAST) peer-link: `%s` (%s)",
                  accountId, outgoingLinkSettings.url()
              )
          );
        }
      }
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        logger.warn(String.format("Unable to connect to ILP-over-HTTP (BLAST) peer-link: `%s` (%s): %s",
            accountId, outgoingLinkSettings.url(), e.getMessage())
        );
      } else {
        throw new HttpClientErrorException(
            e.getStatusCode(),
            String.format("Unable to connect to ILP-over-HTTP (BLAST) peer-link: `%s` (%s): %s",
                accountId, outgoingLinkSettings.url(), e.getStatusText()
            )
        );
      }
    } catch (ResourceAccessException e) { // Remote endpoint is not serving...
      logger.warn(
          String.format("Unable to connect to ILP-over-HTTP (BLAST) peer-link: `%s` (%s): %s",
              accountId, outgoingLinkSettings.url(), e.getMessage())
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

    headers.setBearerAuth(this.constructAuthToken());
    return headers;
  }

  /**
   * Construct an authentication token that can be used for an outgoing request.
   *
   * @return A byte-array containing the auth token.
   */
  protected abstract String constructAuthToken();

  protected Supplier<Optional<InterledgerAddress>> getOperatorAddressSupplier() {
    return operatorAddressSupplier;
  }

  protected RestTemplate getRestTemplate() {
    return restTemplate;
  }

  protected OutgoingLinkSettings getOutgoingLinkSettings() {
    return outgoingLinkSettings;
  }

  /**
   * Helper-method to de-reference the operator address.
   */
  protected InterledgerAddress operatorAddressSupplier() {
    return operatorAddressSupplier.get().get();
  }

}
