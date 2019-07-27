package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

import okhttp3.HttpUrl;
import org.interledger.connector.accounts.SettlementEngineDetails;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class SettlementEngineDetailsFromPropertyFile implements SettlementEngineDetails {

  private String settlementEngineAccountId;
  private int assetScale;
  private String baseUrl;

  public String getSettlementEngineAccountId() {
    return settlementEngineAccountId;
  }

  public void setSettlementEngineAccountId(String settlementEngineAccountId) {
    this.settlementEngineAccountId = settlementEngineAccountId;
  }

  public int getAssetScale() {
    return assetScale;
  }

  public void setAssetScale(int assetScale) {
    this.assetScale = assetScale;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public String settlementEngineAccountId() {
    return this.getSettlementEngineAccountId();
  }


  @Override
  public int assetScale() {
    return this.getAssetScale();
  }

  @Override
  public HttpUrl baseUrl() {
    return HttpUrl.parse(getBaseUrl());
  }
}
