package org.interledger.connector.opay.model;

import org.interledger.connector.opay.ImmutableOpenPaymentsMetadata;
import org.interledger.stream.Denomination;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableOpenPaymentsMetadata.class)
@JsonDeserialize(as = ImmutableOpenPaymentsMetadata.class)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public interface OpenPaymentsMetadata {

  static ImmutableOpenPaymentsMetadata.Builder builder() {
    return ImmutableOpenPaymentsMetadata.builder();
  }

  /**
   * URL of the Open Payments server's sessions endpoint where the client is able to create new sessions with the subject.
   *
   * @return an {@link HttpUrl} representing the URL of the issuer.
   */
  HttpUrl issuer();

  /**
   * URL of the Authorization Server that can authorize new sessions with the issuer.
   *
   * This should be the base URL of the Authorization Server API, and should not give any information
   * on specific endpoints on the AS.
   *
   * @return An {@link HttpUrl} representing the base URL for the Authorization Server.
   */
  HttpUrl authorizationIssuer();

  /**
   * Endpoint on the AS where the client can create an authorization grant.
   *
   * In the case of Open Payments, the client is usually a merchant who will need to complete an OAuth2 flow in order
   * to receive an authorization grant from the resource owner, in this case the sender or customer.
   *
   * @see "https://tools.ietf.org/html/rfc6749"
   * @return the {@link HttpUrl} of the endpoint on the Authorization Server which can create authorization grants.
   */
  HttpUrl authorizationEndpoint();

  /**
   * Endpoint on the AS where the client can request access tokens and refresh tokens.
   *
   * In the case of Open Payments, the client is usually a merchant. Once the resource owner (sender) has given
   * consent to the merchant to gain an authorization grant, the merchant can then access this endpoint on the
   * AS to retrieve access tokens and refresh tokens, as defined in the OAuth2 RFC (https://tools.ietf.org/html/rfc6749).
   *
   * @see "https://tools.ietf.org/html/rfc6749"
   * @return The {@link HttpUrl} of the endpoint on the Authorization Server which gives clients access and refresh tokens.
   */
  HttpUrl tokenEndpoint();

  /**
   * URL of the Open Payments server's invoices endpoint.
   *
   * Clients can send requests to this endpoint to create and manage invoices on the Open Payments server.
   *
   * @return The {@link HttpUrl} of the endpoint on the Open Payments Server which handles invoices.
   */
  HttpUrl invoicesEndpoint();

  /**
   * URL of the Open Payments server's mandates endpoint.
   *
   * Clients can send requests to this endpoint to create and manage mandates on the Open Payments server.
   *
   * @return The {@link HttpUrl} of the endpoint on the Open Payments Server which handles mandates.
   */
  HttpUrl mandatesUrl();

  /**
   * @return A list of {@link Denomination} for assets that can be used to create agreements on this server
   */
  List<Denomination> assetsSupported();
}
