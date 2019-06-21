package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

import com.google.common.collect.Maps;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.LinkType;

import java.util.Map;
import java.util.Optional;

/**
 * Models the YAML format for spring-boot automatic configuration property loading.
 */
public class AccountSettingsFromPropertyFile implements AccountSettings {

  private AccountId id;
  private Optional<String> ilpAddressSegment;
  private String description = "";
  private AccountRelationship relationship = AccountRelationship.CHILD;

  private boolean internal;
  private boolean connectionInitiator;
  private boolean sendRoutes;
  private boolean receiveRoutes;

  private String assetCode = "USD";
  private int assetScale = 2;

  private AccountBalanceSettingsFromPropertyFile balanceSettings = new AccountBalanceSettingsFromPropertyFile();
  private AccountRateLimitSettingsFromPropertyFile rateLimitSettings = new AccountRateLimitSettingsFromPropertyFile();

  private LinkType linkType;
  private Map<String, Object> customSettings = Maps.newConcurrentMap();
  private Optional<Long> maximumPacketAmount = Optional.empty();

  @Override
  public AccountId getAccountId() {
    return id;
  }

  public void setId(AccountId id) {
    this.id = id;
  }

  @Override
  public boolean isInternal() {
    return internal;
  }

  public void setInternal(boolean internal) {
    this.internal = internal;
  }

  @Override
  public boolean isConnectionInitiator() {
    return connectionInitiator;
  }

  public void setConnectionInitiator(boolean connectionInitiator) {
    this.connectionInitiator = connectionInitiator;
  }

  @Override
  public Optional<String> getIlpAddressSegment() {
    return ilpAddressSegment;
  }

  public void setIlpAddressSegment(Optional<String> ilpAddressSegment) {
    this.ilpAddressSegment = ilpAddressSegment;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public AccountRelationship getAccountRelationship() {
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

  public AccountRateLimitSettingsFromPropertyFile getRateLimitSettings() {
    return rateLimitSettings;
  }

  public void setRateLimitSettings(AccountRateLimitSettingsFromPropertyFile rateLimitSettings) {
    this.rateLimitSettings = rateLimitSettings;
  }

  @Override
  public LinkType getLinkType() {
    return linkType;
  }

  public void setLinkType(LinkType linkType) {
    this.linkType = linkType;
  }

  @Override
  public Map<String, Object> getCustomSettings() {
    return customSettings;
  }

  public void setCustomSettings(Map<String, Object> customSettings) {
    this.customSettings = customSettings;
  }

  @Override
  public Optional<Long> getMaximumPacketAmount() {
    return maximumPacketAmount;
  }

  public void setMaximumPacketAmount(Optional<Long> maximumPacketAmount) {
    this.maximumPacketAmount = maximumPacketAmount;
  }
}
