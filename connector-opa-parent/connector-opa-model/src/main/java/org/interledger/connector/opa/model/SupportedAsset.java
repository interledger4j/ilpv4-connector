package org.interledger.connector.opa.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SupportedAsset that = (SupportedAsset) o;
    return assetScale == that.assetScale &&
      Objects.equals(assetCode, that.assetCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assetCode, assetScale);
  }
}
