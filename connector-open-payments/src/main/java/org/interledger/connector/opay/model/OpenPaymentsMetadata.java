package org.interledger.connector.opay.model;

import org.interledger.connector.opay.controllers.constants.PathConstants;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
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
  @JsonProperty("issuer")
  HttpUrl issuer();

  /**
   * URL of the Authorization Server that can authorize new sessions with the issuer.
   *
   * This should be the base URL of the Authorization Server API, and should not give any information
   * on specific endpoints on the AS.
   *
   * @return An {@link HttpUrl} representing the base URL for the Authorization Server.
   */
  @JsonProperty("authorization_issuer")
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
  @Value.Derived
  @JsonProperty("authorization_endpoint")
  default HttpUrl authorizationEndpoint() {
    return authorizationIssuer().newBuilder().addPathSegment(PathConstants.AUTHORIZE).build();
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
  @Value.Derived
  @JsonProperty("token_endpoint")
  default HttpUrl tokenEndpoint() {
    return authorizationIssuer().newBuilder().addPathSegment(PathConstants.TOKEN).build();
  };

  /**
   * URL of the Open Payments server's invoices endpoint.
   *
   * Clients can send requests to this endpoint to create and manage invoices on the Open Payments server.
   *
   * @return The {@link HttpUrl} of the endpoint on the Open Payments Server which handles invoices.
   */
  @Value.Derived
  @JsonProperty("invoices_endpoint")
  default HttpUrl invoicesEndpoint() {
    return issuer().newBuilder().addPathSegment(PathConstants.INVOICE).build();
  };

  /**
   * URL of the Open Payments server's mandates endpoint.
   *
   * Clients can send requests to this endpoint to create and manage mandates on the Open Payments server.
   *
   * @return The {@link HttpUrl} of the endpoint on the Open Payments Server which handles mandates.
   */
  @Value.Derived
  @JsonProperty("mandates_endpoint")
  default HttpUrl mandatesEndpoint() {
    return issuer().newBuilder().addPathSegment(PathConstants.MANDATE).build();
  };

  /**
   * @return A list of {@link SupportedAsset} for assets that can be used to create agreements on this server
   */
  @JsonProperty("assets_supported")
  List<SupportedAsset> assetsSupported();

  /**
   * Only need this so that {@link org.interledger.connector.opay.config.OpenPaymentsMetadataFromPropertyFile} can
   * make super calls to OpenPaymentsMetdata
   */
  abstract class AbstractOpenPaymentsMetadata implements OpenPaymentsMetadata {

  }
}
