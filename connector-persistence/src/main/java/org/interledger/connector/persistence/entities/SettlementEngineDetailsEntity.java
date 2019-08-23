package org.interledger.connector.persistence.entities;

import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.persistence.HashMapConverter;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import java.util.Map;
import java.util.Objects;

import static org.interledger.connector.persistence.entities.DataConstants.ColumnNames.SE_ACCOUNT_ID;

@Access(AccessType.FIELD)
@Embeddable
public class SettlementEngineDetailsEntity {

  @Column(name = SE_ACCOUNT_ID, unique = true)
  private String settlementEngineAccountId;

  @Column(name = "SE_BASE_URL")
  private String baseUrl;

  @Convert(converter = HashMapConverter.class)
  @Column(name = "SE_CUSTOM_SETTINGS", length = 8196)
  private Map<String, Object> customSettings;

  /**
   * To satisfy Hibernate
   */
  SettlementEngineDetailsEntity() {
  }

  public SettlementEngineDetailsEntity(final SettlementEngineDetails settlementEngineDetails) {
    Objects.requireNonNull(settlementEngineDetails);

    this.setSettlementEngineAccountId(
      settlementEngineDetails.settlementEngineAccountId().map(SettlementEngineAccountId::value).orElse(null));
    this.setBaseUrl(settlementEngineDetails.baseUrl().toString());
    this.setCustomSettings(settlementEngineDetails.customSettings());
  }

  public String getSettlementEngineAccountId() {
    return settlementEngineAccountId;
  }

  public void setSettlementEngineAccountId(String settlementEngineAccountId) {
    this.settlementEngineAccountId = settlementEngineAccountId;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Map<String, Object> getCustomSettings() {
    return customSettings;
  }

  public void setCustomSettings(Map<String, Object> customSettings) {
    this.customSettings = customSettings;
  }
}
