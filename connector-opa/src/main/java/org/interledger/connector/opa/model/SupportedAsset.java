package org.interledger.connector.opa.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Defines an asset denomination which is supported by this Open Payments server by providing an asset code and scale.
 *
 * Must be a POJO, not an immutable, to be able to accurately populate this class from a configuration file.
 */
public class SupportedAsset {

  /**
   * Currency code or other asset identifier. For example, `USD`, `EUR`, or `BTC`.
   */
  @JsonProperty("code")
  private String assetCode;

  /**
   * <p>An asset scale is the difference, in orders of magnitude, between an asset's `standard unit` and a
   * corresponding `fractional unit`.</p>
   *
   * <p>A standard unit represents the typical unit of account for a particular asset. For example 1 USD in the case of
   * U.S. dollars, or 1 BTC in the case of Bitcoin (Note that peers are free to define this value in any way, but
   * participants in an Interledger accounting relationship must be sure to use the same value. Thus, it is suggested to
   * use typical values when possible).</p>
   *
   * <p>A fractional unit represents some unit smaller than its corresponding standard unit, but with greater
   * precision. Examples of fractional monetary units include one cent ($0.01 USD), or 1 satoshi (0.00000001 BTC).</p>
   *
   * <p>Because Interledger amounts are integers, but most currencies are typically represented as fractional units
   * (e.g. cents), this property defines how many Interledger units make up one standard unit of the asset code
   * specified above.</p>
   *
   * <p>More formally, the asset scale is a non-negative integer (0, 1, 2, …) such that one standard unit equals
   * 10^(-scale) of a corresponding fractional unit. If the fractional unit equals the standard unit, then the asset
   * scale is 0.</p>
   *
   * <p>For example, one "cent" represents an asset scale of 2 in the case of USD; 1 satoshi represents an asset scale
   * of 8 in the case of Bitcoin; and 1 drop represents an asset scale of 6 in XRP.</p>
   */
  @JsonProperty("scale")
  private short assetScale;

  /**
   * No arg constructor.
   */
  public SupportedAsset() { }

  /**
   * Required args constructor.
   *
   * @param assetCode A {@link String} representing the currency code that is supported.
   * @param assetScale A short which represents the asset scale that is supported.
   */
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
