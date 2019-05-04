package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

import com.google.common.collect.Maps;
import org.interledger.connector.accounts.AccountProviderId;
import org.interledger.connector.accounts.AccountProviderSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.link.LinkType;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 *
 * @deprecated The AccountProvider concept is no longer necessary, and will be removed in a future version.
 */
@Deprecated
public class AccountProviderSettingsFromPropertyFile implements AccountProviderSettings {

  private AccountProviderId id;
  private Optional<String> ilpAddressSegment;
  private String description = "";
  private AccountRelationship relationship = AccountRelationship.CHILD;

  private boolean sendRoutes;
  private boolean receiveRoutes;

  private String assetCode = "USD";
  private int assetScale = 2;

  private AccountBalanceSettingsFromPropertyFile balanceSettings = new AccountBalanceSettingsFromPropertyFile();
  private AccountRateLimitSettingsFromPropertyFile rateLimitSettings = new AccountRateLimitSettingsFromPropertyFile();

  private LinkType LinkType;
  private Map<String, Object> customSettings = Maps.newConcurrentMap();
  private Optional<BigInteger> maximumPacketAmount = Optional.empty();

  @Override
  public AccountProviderId getId() {
    return id;
  }

  public void setId(AccountProviderId id) {
    this.id = id;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public Optional<String> getIlpAddressSegment() {
    return ilpAddressSegment;
  }

  public void setIlpAddressSegment(Optional<String> ilpAddressSegment) {
    this.ilpAddressSegment = ilpAddressSegment;
  }

  @Override
  public AccountRelationship getRelationship() {
    return relationship;
  }

  public void setRelationship(AccountRelationship relationship) {
    this.relationship = relationship;
  }

  @Override
  public boolean isSendRoutes() {
    return sendRoutes;
  }

  public void setSendRoutes(boolean sendRoutes) {
    this.sendRoutes = sendRoutes;
  }

  @Override
  public boolean isReceiveRoutes() {
    return receiveRoutes;
  }

  public void setReceiveRoutes(boolean receiveRoutes) {
    this.receiveRoutes = receiveRoutes;
  }

  @Override
  public String getAssetCode() {
    return assetCode;
  }

  public void setAssetCode(String assetCode) {
    this.assetCode = assetCode;
  }

  @Override
  public int getAssetScale() {
    return assetScale;
  }

  public void setAssetScale(int assetScale) {
    this.assetScale = assetScale;
  }

  @Override
  public AccountBalanceSettingsFromPropertyFile getBalanceSettings() {
    return balanceSettings;
  }

  public void setBalanceSettings(AccountBalanceSettingsFromPropertyFile balanceSettings) {
    this.balanceSettings = balanceSettings;
  }

  @Override
  public AccountRateLimitSettingsFromPropertyFile getRateLimitSettings() {
    return rateLimitSettings;
  }

  public void setRateLimitSettings(AccountRateLimitSettingsFromPropertyFile rateLimitSettings) {
    this.rateLimitSettings = rateLimitSettings;
  }

  @Override
  public LinkType getLinkType() {
    return LinkType;
  }

  public void setLinkType(LinkType LinkType) {
    this.LinkType = LinkType;
  }

  @Override
  public Map<String, Object> getCustomSettings() {
    return customSettings;
  }

  public void setCustomSettings(Map<String, Object> customSettings) {
    this.customSettings = customSettings;
  }

  @Override
  public Optional<BigInteger> getMaximumPacketAmount() {
    return maximumPacketAmount;
  }

  public void setMaximumPacketAmount(Optional<BigInteger> maximumPacketAmount) {
    this.maximumPacketAmount = maximumPacketAmount;
  }
}
