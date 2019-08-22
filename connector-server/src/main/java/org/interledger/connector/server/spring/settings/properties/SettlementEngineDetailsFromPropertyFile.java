package org.interledger.connector.server.spring.settings.properties;

import okhttp3.HttpUrl;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineDetails;

import java.util.Map;
import java.util.Optional;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 *
 * @deprecated Will go away once https://github.com/sappenin/java-ilpv4-connector/issues/103 is fixed.
 */
@Deprecated
public class SettlementEngineDetailsFromPropertyFile implements SettlementEngineDetails {

  private String settlementEngineAccountId;
  private String baseUrl;
  private Map<String, Object> customSettings;

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

  @Override
  public Optional<SettlementEngineAccountId> settlementEngineAccountId() {
    return settlementEngineAccountId == null ? Optional.empty() :
      Optional.of(SettlementEngineAccountId.of(settlementEngineAccountId));
  }

  @Override
  public HttpUrl baseUrl() {
    return HttpUrl.parse(getBaseUrl());
  }

  @Override
  public Map<String, Object> customSettings() {
    return this.customSettings;
  }

}
