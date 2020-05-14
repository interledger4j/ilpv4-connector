package org.interledger.connector.settings.properties;

import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.connector.opa.model.SupportedAsset;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Pojo class for automatic mapping of configuration properties via Spring's {@link ConfigurationProperties}
 * annotation to {@link OpenPaymentsMetadata}.
 */
@JsonSerialize(as = OpenPaymentsMetadata.class)
public class OpenPaymentsMetadataFromPropertyFile implements OpenPaymentsMetadata {

  private HttpUrl issuer;
  private HttpUrl authorizationIssuer;
  private HttpUrl authorizationEndpoint;
  private HttpUrl tokenEndpoint;
  private HttpUrl invoicesEndpoint;
  private HttpUrl mandatesEndpoint;
  private HttpUrl accountServicer;
  private List<SupportedAsset> assetsSupported;

  @Override
  public HttpUrl issuer() {
    return issuer;
  }

  @Override
  public HttpUrl authorizationIssuer() {
    return authorizationIssuer;
  }

  @Override
  public HttpUrl authorizationEndpoint() {
    return authorizationIssuer().newBuilder().addPathSegment(OpenPaymentsPathConstants.AUTHORIZE).build();
  }

  @Override
  public HttpUrl tokenEndpoint() {
    return authorizationIssuer().newBuilder().addPathSegment(OpenPaymentsPathConstants.TOKEN).build();
  }

  @Override
  public HttpUrl invoicesEndpoint() {
    return issuer().newBuilder().addPathSegment(OpenPaymentsPathConstants.INVOICES).build();
  }

  @Override
  public HttpUrl mandatesEndpoint() {
    return issuer().newBuilder().addPathSegment(OpenPaymentsPathConstants.MANDATES).build();
  }

  @Override
  public HttpUrl accountServicer() {
    return accountServicer;
  }

  @Override
  public List<SupportedAsset> assetsSupported() {
    return assetsSupported;
  }

  public void setIssuer(HttpUrl issuer) {
    this.issuer = issuer;
  }

  public void setAuthorizationIssuer(HttpUrl authorizationIssuer) {
    this.authorizationIssuer = authorizationIssuer;
  }

  public void setAuthorizationEndpoint(HttpUrl authorizationEndpoint) {
    this.authorizationEndpoint = authorizationEndpoint;
  }

  public void setTokenEndpoint(HttpUrl tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }

  public void setInvoicesEndpoint(HttpUrl invoicesEndpoint) {
    this.invoicesEndpoint = invoicesEndpoint;
  }

  public void setMandatesEndpoint(HttpUrl mandatesEndpoint) {
    this.mandatesEndpoint = mandatesEndpoint;
  }

  public void setAssetsSupported(List<SupportedAsset> assetsSupported) {
    this.assetsSupported = assetsSupported;
  }

  public void setAccountServicer(HttpUrl accountServicer) {
    this.accountServicer = accountServicer;
  }
}
