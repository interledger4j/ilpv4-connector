package org.interledger.connector.opay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SupportedAsset {

  @JsonProperty("code")
  private String assetCode;

  @JsonProperty("scale")
  private short assetScale;

  public SupportedAsset() {
  }

  public SupportedAsset(String assetCode, short assetScale) {
    this.assetCode = assetCode;
    this.assetScale = assetScale;
  }

  public String assetCode() {
    return assetCode;
  }

  public void setAssetCode(String assetCode) {
    this.assetCode = assetCode;
  }

  public short assetScale() {
    return assetScale;
  }

  public void setAssetScale(short assetScale) {
    this.assetScale = assetScale;
  }
}
