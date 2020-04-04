package org.interledger.connector.opay.config;

import org.interledger.connector.opay.model.OpenPaymentsMetadata;
import org.interledger.connector.opay.model.SupportedAsset;

import okhttp3.HttpUrl;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "interledger.connector.open-payments")
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
    return authorizationEndpoint;
  }

  @Override
  public HttpUrl tokenEndpoint() {
    return tokenEndpoint;
  }

  @Override
  public HttpUrl invoicesEndpoint() {
    return invoicesEndpoint;
  }

  @Override
  public HttpUrl mandatesEndpoint() {
    return mandatesEndpoint;
  }

  @Override
  public List<SupportedAsset> assetsSupported() {
    return assetsSupported;
  }

  public HttpUrl getIssuer() {
    return issuer;
  }

  public void setIssuer(HttpUrl issuer) {
    this.issuer = issuer;
  }

  public HttpUrl getAuthorizationIssuer() {
    return authorizationIssuer;
  }

  public void setAuthorizationIssuer(HttpUrl authorizationIssuer) {
    this.authorizationIssuer = authorizationIssuer;
  }

  public HttpUrl getAuthorizationEndpoint() {
    return authorizationEndpoint;
  }

  public void setAuthorizationEndpoint(HttpUrl authorizationEndpoint) {
    this.authorizationEndpoint = authorizationEndpoint;
  }

  public HttpUrl getTokenEndpoint() {
    return tokenEndpoint;
  }

  public void setTokenEndpoint(HttpUrl tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }

  public HttpUrl getInvoicesEndpoint() {
    return invoicesEndpoint;
  }

  public void setInvoicesEndpoint(HttpUrl invoicesEndpoint) {
    this.invoicesEndpoint = invoicesEndpoint;
  }

  public HttpUrl getMandatesEndpoint() {
    return mandatesEndpoint;
  }

  public void setMandatesEndpoint(HttpUrl mandatesEndpoint) {
    this.mandatesEndpoint = mandatesEndpoint;
  }

  public List<SupportedAsset> getAssetsSupported() {
    return assetsSupported;
  }

  public void setAssetsSupported(List<SupportedAsset> assetsSupported) {
    this.assetsSupported = assetsSupported;
  }
}
