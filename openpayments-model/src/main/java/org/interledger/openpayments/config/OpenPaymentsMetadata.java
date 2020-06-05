package org.interledger.openpayments.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.util.List;

/**
 * A configurable settings class which holds information about this Open Payments server.
 */
@Value.Immutable(intern = true)
@JsonSerialize(as = ImmutableOpenPaymentsMetadata.class)
@JsonDeserialize(as = ImmutableOpenPaymentsMetadata.class)
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
   * The default scheme used when deriving a URL for an invoice.
   * @return a compatible URL scheme
   */
  @Value.Default
  @JsonProperty
  default String defaultScheme() {
    return "https";
  }

  /**
   * URL of the Authorization Server that can authorize new sessions with the issuer.
   *
   * This should be the base URL of the Authorization Server API, and should not give any information
   * on specific endpoints on the AS.
   *
   * If {@link OpenPaymentsMetadata#issuer()} is also the Authorization Issuer,
   * this will default to {@link OpenPaymentsMetadata#issuer()}.
   *
   * @return An {@link HttpUrl} representing the base URL for the Authorization Server.
   */
  @Value.Default
  @JsonProperty("authorization_issuer")
  default HttpUrl authorizationIssuer() {
    return this.issuer();
  };

  /**
   * Endpoint on the AS where the client can create an authorization grant.
   *
   * In the case of Open Payments, the client is usually a merchant who will need to complete an OAuth2 flow in order
   * to receive an authorization grant from the resource owner, in this case the sender or customer.
   *
   * @see "https://tools.ietf.org/html/rfc6749"
   * @return the {@link HttpUrl} of the endpoint on the Authorization Server which can create authorization grants.
   */
  @Value.Default
  @JsonProperty("authorization_endpoint")
  default HttpUrl authorizationEndpoint() {
    return authorizationIssuer().newBuilder().addPathSegment("authorize").build();
  };

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
  @Value.Default
  @JsonProperty("token_endpoint")
  default HttpUrl tokenEndpoint() {
    return authorizationIssuer().newBuilder().addPathSegment("token").build();
  };

  /**
   * URL of the Open Payments server's invoices endpoint.
   *
   * Clients can send requests to this endpoint to create and manage invoices on the Open Payments server.
   *
   * @return The {@link HttpUrl} of the endpoint on the Open Payments Server which handles invoices.
   */
  @Value.Default
  @JsonProperty("invoices_endpoint")
  default HttpUrl invoicesEndpoint() {
    return issuer().newBuilder().addPathSegment("invoices").build();
  };

  /**
   * URL of the Open Payments server's mandates endpoint.
   *
   * Clients can send requests to this endpoint to create and manage mandates on the Open Payments server.
   *
   * @return The {@link HttpUrl} of the endpoint on the Open Payments Server which handles mandates.
   */
  @Value.Default
  @JsonProperty("mandates_endpoint")
  default HttpUrl mandatesEndpoint() {
    return issuer().newBuilder().addPathSegment("mandates").build();
  };

  /**
   * The {@link HttpUrl} of the servicer of the account which created the invoice.
   *
   * This could be the URL of a wallet front end, which can be redirected to in order to complete a checkout flow.
   *
   * @return The {@link HttpUrl} of the servicer of this {@link Invoice}.
   */
  @Value.Default
  @JsonProperty("accountServicer")
  default HttpUrl accountServicer() { return issuer(); }

  /**
   * @return A list of {@link SupportedAsset} for assets that can be used to create agreements on this server
   */
  @JsonProperty("assets_supported")
  List<SupportedAsset> assetsSupported();
}
