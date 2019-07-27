package org.interledger.ilpv4.connector.persistence.entities;

import okhttp3.HttpUrl;
import org.interledger.connector.accounts.SettlementEngineDetails;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Access(AccessType.FIELD)
@Embeddable
public class SettlementEngineDetailsEntity implements SettlementEngineDetails {

  // See Javadoc in AccountSettings for more details around the number types in this class.

  @Column(name = "SE_ACCOUNT_ID", unique = true)
  String accountId;

  @Column(name = "SE_ASSET_SCALE")
  int assetScale;

  @Column(name = "SE_BASE_URL")
  String baseUrl;

  /**
   * To satisfy Hibernate
   */
  SettlementEngineDetailsEntity() {
  }

  public SettlementEngineDetailsEntity(SettlementEngineDetails settlementEngineDetails) {
    this.setAccountId(settlementEngineDetails.settlementEngineAccountId());
    this.setAssetScale(settlementEngineDetails.assetScale());
    this.setBaseUrl(settlementEngineDetails.baseUrl().toString());
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
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

  /**
   * @deprecated Will be removed once #217 is fixed.
   */
  @Override
  @Deprecated
  public String settlementEngineAccountId() {
    return this.getAccountId();
  }

  /**
   * @deprecated Will be removed once #217 is fixed.
   */
  @Override
  @Deprecated
  public int assetScale() {
    return this.getAssetScale();
  }

  /**
   * @deprecated Will be removed once #217 is fixed.
   */
  @Override
  @Deprecated
  public HttpUrl baseUrl() {
    if (this.getBaseUrl() == null) {
      return null;
    } else {
      return HttpUrl.parse(this.getBaseUrl());
    }
  }
}
