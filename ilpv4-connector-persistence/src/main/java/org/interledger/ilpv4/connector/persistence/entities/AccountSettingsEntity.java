package org.interledger.ilpv4.connector.persistence.entities;

import org.hibernate.annotations.NaturalId;
import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.ilpv4.connector.persistence.AccountRelationshipConverter;
import org.interledger.ilpv4.connector.persistence.HashMapConverter;
import org.interledger.ilpv4.connector.persistence.LinkTypeConverter;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.interledger.ilpv4.connector.persistence.entities.DataConstants.ColumnNames.ACCOUNT_RELATIONSHIP;
import static org.interledger.ilpv4.connector.persistence.entities.DataConstants.IndexNames.ACCT_REL_IDX;
import static org.interledger.ilpv4.connector.persistence.entities.DataConstants.TableNames.ACCOUNT_SETTINGS;

@Entity
@Access(AccessType.FIELD)
@Table(name = ACCOUNT_SETTINGS, indexes = {
  @Index(name = ACCT_REL_IDX, columnList = ACCOUNT_RELATIONSHIP)
})
public class AccountSettingsEntity extends AbstractEntity implements AccountSettings {

  @Id
  @GeneratedValue
  @Column(name = "ID")
  private Long id;

  @NaturalId
  @Column(name = "NATURAL_ID")
  private String naturalId;

  private String description;

  private boolean internal;

  @Column(name = "CONNECTION_INITIATOR")
  private boolean connectionInitiator;

  @Column(name = "ILP_ADDR_SEGMENT")
  private String ilpAddressSegment;

  @Column(name = "ACCOUNT_RELATIONSHIP")
  @Convert(converter = AccountRelationshipConverter.class)
  private AccountRelationship accountRelationship;

  @Column(name = "LINK_TYPE")
  @Convert(converter = LinkTypeConverter.class)
  private LinkType linkType;

  @Column(name = "ASSET_CODE")
  private String assetCode;

  @Column(name = "ASSET_SCALE")
  private int assetScale;

  @Column(name = "MAX_PACKET_AMT")
  private Long maximumPacketAmount;

  @Column(name = "SEND_ROUTES")
  private boolean sendRoutes;

  @Column(name = "RECEIVE_ROUTES")
  private boolean receiveRoutes;

  @Embedded
  private AccountBalanceSettingsEntity balanceSettings;

  @Embedded
  private AccountRateLimitSettingsEntity rateLimitSettings;

  @Convert(converter = HashMapConverter.class)
  @Column(name = "CUSTOM_SETTINGS", length = 8196)
  private Map<String, Object> customSettings;

  /**
   * For Hibernate....
   */
  AccountSettingsEntity() {
  }

  public AccountSettingsEntity(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    this.naturalId = accountSettings.getAccountId().value();
    this.description = accountSettings.getDescription();
    this.internal = accountSettings.isInternal();
    this.connectionInitiator = accountSettings.isConnectionInitiator();
    this.ilpAddressSegment = accountSettings.getIlpAddressSegment().orElse(null);
    this.accountRelationship = accountSettings.getAccountRelationship();
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
    return AccountId.of(getNaturalId());
  }

  public Long getId() {
    return this.id;
  }

  public String getNaturalId() {
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
  public boolean isConnectionInitiator() {
    return connectionInitiator;
  }

  public void setConnectionInitiator(boolean connectionInitiator) {
    this.connectionInitiator = connectionInitiator;
  }

  @Override
  public Optional<String> getIlpAddressSegment() {
    return Optional.ofNullable(ilpAddressSegment);
  }

  public void setIlpAddressSegment(Optional<String> ilpAddressSegment) {
    this.ilpAddressSegment = ilpAddressSegment.orElse(null);
  }

  @Override
  public AccountRelationship getAccountRelationship() {
    return accountRelationship;
  }

  public void setAccountRelationship(AccountRelationship accountRelationship) {
    this.accountRelationship = accountRelationship;
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
  public Optional<Long> getMaximumPacketAmount() {
    return Optional.ofNullable(maximumPacketAmount);
  }

  public void setMaximumPacketAmount(Optional<Long> maximumPacketAmount) {
    this.maximumPacketAmount = maximumPacketAmount.orElse(null);
  }

  @Override
  public AccountBalanceSettings getBalanceSettings() {
    return balanceSettings;
  }

  public void setBalanceSettings(AccountBalanceSettingsEntity balanceSettings) {
    this.balanceSettings = balanceSettings;
  }

  public AccountBalanceSettingsEntity getBalanceSettingsEntity() {
    return balanceSettings;
  }

  @Override
  public AccountRateLimitSettings getRateLimitSettings() {
    return rateLimitSettings;
  }

  public void setRateLimitSettings(AccountRateLimitSettingsEntity rateLimitSettings) {
    this.rateLimitSettings = rateLimitSettings;
  }

  public AccountRateLimitSettingsEntity getRateLimitSettingsEntity() {
    return rateLimitSettings;
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


