package org.interledger.connector.opa.config;


import org.interledger.connector.opa.controllers.constants.PathConstants;
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
    return authorizationIssuer().newBuilder().addPathSegment(PathConstants.AUTHORIZE).build();
  }

  @Override
  public HttpUrl tokenEndpoint() {
    return authorizationIssuer().newBuilder().addPathSegment(PathConstants.TOKEN).build();
  }

  @Override
  public HttpUrl invoicesEndpoint() {
    return issuer().newBuilder().addPathSegment(PathConstants.INVOICE).build();
  }

  @Override
  public HttpUrl mandatesEndpoint() {
    return issuer().newBuilder().addPathSegment(PathConstants.MANDATE).build();
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
}
