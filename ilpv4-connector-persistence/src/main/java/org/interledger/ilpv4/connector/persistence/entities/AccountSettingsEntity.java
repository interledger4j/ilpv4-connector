package org.interledger.ilpv4.connector.persistence.entities;

import org.hibernate.annotations.NaturalId;
import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.ilpv4.connector.persistence.HashMapConverter;
import org.interledger.ilpv4.connector.persistence.LinkTypeConverter;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.core.RedisHash;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity.ACCOUNT_SETTINGS;

@Entity
@EnableJpaAuditing
@Access(AccessType.FIELD)
@Table(name = ACCOUNT_SETTINGS)
@RedisHash(ACCOUNT_SETTINGS)
public class AccountSettingsEntity implements AccountSettings {

  public static final String ACCOUNT_SETTINGS = "ACCOUNT_SETTINGS";

  @Id
  @GeneratedValue
  @Column(name = "ID")
  private Long id;

  @NaturalId
  @Column(name = "NATURAL_ID")
  private UUID naturalId;

  private String description;
  private boolean internal;
  private boolean preconfigured;

  @Column(name = "ILP_ADDR_SEGMENT")
  private String ilpAddressSegment;

  private AccountRelationship relationship;

  @Column(name = "LINK_TYPE")
  @Convert(converter = LinkTypeConverter.class)
  private LinkType linkType;

  @Column(name = "ASSET_CODE")
  private String assetCode;

  @Column(name = "ASSET_SCALE")
  private int assetScale;

  @Column(name = "MAX_PACKET_AMT")
  private BigInteger maximumPacketAmount;

  @Embedded
  private AccountBalanceSettingsEntity balanceSettings;

  @Embedded
  private AccountRateLimitSettingsEntity rateLimitSettings;

  private boolean sendRoutes;
  private boolean receiveRoutes;

  @Convert(converter = HashMapConverter.class)
  private Map<String, Object> customSettings;

  /**
   * For Hibernate....
   */
  AccountSettingsEntity() {
  }

  public AccountSettingsEntity(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    // TODO: Consider making AccountId a UUID, or else make naturalId a String.
    this.naturalId = UUID.fromString(accountSettings.getAccountId().value());

    this.description = accountSettings.getDescription();
    this.internal = accountSettings.isInternal();
    this.preconfigured = accountSettings.isPreconfigured();
    this.ilpAddressSegment = accountSettings.getIlpAddressSegment().orElse(null);
    this.relationship = accountSettings.getRelationship();
    this.linkType = accountSettings.getLinkType();
    this.assetCode = accountSettings.getAssetCode();
    this.assetScale = accountSettings.getAssetScale();
    this.maximumPacketAmount = accountSettings.getMaximumPacketAmount().orElse(null);
    this.balanceSettings = new AccountBalanceSettingsEntity(accountSettings.getBalanceSettings());
    this.rateLimitSettings = new AccountRateLimitSettingsEntity(accountSettings.getRateLimitSettings());
    this.sendRoutes = accountSettings.isSendRoutes();
    this.receiveRoutes = accountSettings.isReceiveRoutes();
    this.customSettings = accountSettings.getCustomSettings();
  }

  @Override
  public AccountId getAccountId() {
    return AccountId.of(getNaturalId().toString());
  }

  public Long getId() {
    return this.id;
  }

  public UUID getNaturalId() {
    return naturalId;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean isInternal() {
    return internal;
  }

  public void setInternal(boolean internal) {
    this.internal = internal;
  }

  @Override
  public boolean isPreconfigured() {
    return preconfigured;
  }

  public void setPreconfigured(boolean preconfigured) {
    this.preconfigured = preconfigured;
  }

  @Override
  public Optional<String> getIlpAddressSegment() {
    return Optional.ofNullable(ilpAddressSegment);
  }

  public void setIlpAddressSegment(Optional<String> ilpAddressSegment) {
    this.ilpAddressSegment = ilpAddressSegment.orElse(null);
  }

  @Override
  public AccountRelationship getRelationship() {
    return relationship;
  }

  public void setRelationship(AccountRelationship relationship) {
    this.relationship = relationship;
  }

  @Override
  public LinkType getLinkType() {
    return linkType;
  }

  public void setLinkType(LinkType linkType) {
    this.linkType = linkType;
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
  public Optional<BigInteger> getMaximumPacketAmount() {
    return Optional.ofNullable(maximumPacketAmount);
  }

  public void setMaximumPacketAmount(Optional<BigInteger> maximumPacketAmount) {
    this.maximumPacketAmount = maximumPacketAmount.orElse(null);
  }

  @Override
  public AccountBalanceSettings getBalanceSettings() {
    return balanceSettings;
  }

  public void setBalanceSettings(AccountBalanceSettingsEntity balanceSettings) {
    this.balanceSettings = balanceSettings;
  }

  @Override
  public AccountRateLimitSettings getRateLimitSettings() {
    return rateLimitSettings;
  }

  public void setRateLimitSettings(AccountRateLimitSettingsEntity rateLimitSettings) {
    this.rateLimitSettings = rateLimitSettings;
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
  public Map<String, Object> getCustomSettings() {
    return customSettings;
  }

  public void setCustomSettings(Map<String, Object> customSettings) {
    this.customSettings = customSettings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AccountSettingsEntity that = (AccountSettingsEntity) o;
    return Objects.equals(getNaturalId(), that.getNaturalId());
  }

  /**
   * Return the hashcode of the natural identifier.
   *
   * @return
   *
   * @see "https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/"
   */
  @Override
  public int hashCode() {
    return getNaturalId().hashCode();
  }
}


