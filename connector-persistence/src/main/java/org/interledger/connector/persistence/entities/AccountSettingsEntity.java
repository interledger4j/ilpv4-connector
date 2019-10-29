package org.interledger.connector.persistence.entities;

import org.hibernate.annotations.NaturalId;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.persistence.AccountRelationshipConverter;
import org.interledger.connector.persistence.HashMapConverter;
import org.interledger.connector.persistence.LinkTypeConverter;
import org.interledger.link.LinkType;

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

import static org.interledger.connector.persistence.entities.DataConstants.ColumnNames.ACCOUNT_RELATIONSHIP;
import static org.interledger.connector.persistence.entities.DataConstants.IndexNames.ACCT_REL_IDX;
import static org.interledger.connector.persistence.entities.DataConstants.TableNames.ACCOUNT_SETTINGS;

@Entity
@Access(AccessType.FIELD)
@Table(name = ACCOUNT_SETTINGS, indexes = {
  @Index(name = ACCT_REL_IDX, columnList = ACCOUNT_RELATIONSHIP)
})
@SuppressWarnings({"PMD"})
public class AccountSettingsEntity extends AbstractEntity {

  @Id
  @GeneratedValue
  @Column(name = "ID")
  private Long id;

  @NaturalId
  @Column(name = "NATURAL_ID") // Hibernate treats this as unique, but Liquibase is explicit about uniqueness.
  private String naturalId;

  @Column(name = "DESCRIPTION")
  private String description;

  @Column(name = "INTERNAL")
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

  @Embedded
  private SettlementEngineDetailsEntity settlementEngineDetails;

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

    this.naturalId = accountSettings.accountId().value();
    this.description = accountSettings.description();
    this.internal = accountSettings.isInternal();
    this.connectionInitiator = accountSettings.isConnectionInitiator();
    this.ilpAddressSegment = accountSettings.ilpAddressSegment();
    this.accountRelationship = accountSettings.accountRelationship();
    this.linkType = accountSettings.linkType();
    this.assetCode = accountSettings.assetCode();
    this.assetScale = accountSettings.assetScale();
    this.maximumPacketAmount = accountSettings.maximumPacketAmount().orElse(null);
    this.balanceSettings = new AccountBalanceSettingsEntity(accountSettings.balanceSettings());
    this.rateLimitSettings = new AccountRateLimitSettingsEntity(accountSettings.rateLimitSettings());
    this.settlementEngineDetails =
      accountSettings.settlementEngineDetails().map(SettlementEngineDetailsEntity::new).orElse(null);

    this.sendRoutes = accountSettings.isSendRoutes();
    this.receiveRoutes = accountSettings.isReceiveRoutes();
    this.customSettings = accountSettings.customSettings();
  }

  public AccountId getAccountId() {
    return AccountId.of(getNaturalId());
  }

  public Long getId() {
    return this.id;
  }

  public String getNaturalId() {
    return naturalId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isInternal() {
    return internal;
  }

  public void setInternal(boolean internal) {
    this.internal = internal;
  }

  public boolean isConnectionInitiator() {
    return connectionInitiator;
  }

  public void setConnectionInitiator(boolean connectionInitiator) {
    this.connectionInitiator = connectionInitiator;
  }

  public String getIlpAddressSegment() {
    return ilpAddressSegment;
  }

  public void setIlpAddressSegment(String ilpAddressSegment) {
    this.ilpAddressSegment = ilpAddressSegment;
  }

  public AccountRelationship getAccountRelationship() {
    return accountRelationship;
  }

  public void setAccountRelationship(AccountRelationship accountRelationship) {
    this.accountRelationship = accountRelationship;
  }

  public LinkType getLinkType() {
    return linkType;
  }

  public void setLinkType(LinkType linkType) {
    this.linkType = linkType;
  }

  public String getAssetCode() {
    return assetCode;
  }

  public void setAssetCode(String assetCode) {
    this.assetCode = assetCode;
  }

  public int getAssetScale() {
    return assetScale;
  }

  public void setAssetScale(int assetScale) {
    this.assetScale = assetScale;
  }

  public Optional<Long> getMaximumPacketAmount() {
    return Optional.ofNullable(maximumPacketAmount);
  }

  public void setMaximumPacketAmount(Optional<Long> maximumPacketAmount) {
    this.maximumPacketAmount = maximumPacketAmount.orElse(null);
  }

  public AccountBalanceSettingsEntity getBalanceSettings() {
    return balanceSettings;
  }

  public void setBalanceSettings(AccountBalanceSettingsEntity balanceSettings) {
    this.balanceSettings = balanceSettings;
  }

  public AccountBalanceSettingsEntity getBalanceSettingsEntity() {
    return balanceSettings;
  }

  public AccountRateLimitSettingsEntity getRateLimitSettings() {
    return rateLimitSettings;
  }

  public void setRateLimitSettings(AccountRateLimitSettingsEntity rateLimitSettings) {
    this.rateLimitSettings = rateLimitSettings;
  }

  public AccountRateLimitSettingsEntity getRateLimitSettingsEntity() {
    return rateLimitSettings;
  }

  public SettlementEngineDetailsEntity getSettlementEngineDetailsEntity() {
    return settlementEngineDetails;
  }

  public Optional<SettlementEngineDetailsEntity> settlementEngineDetails() {
    return Optional.ofNullable(settlementEngineDetails);
  }

  public void setSettlementEngineDetails(SettlementEngineDetailsEntity settlementEngineDetails) {
    this.settlementEngineDetails = settlementEngineDetails;
  }

  public boolean isSendRoutes() {
    return sendRoutes;
  }

  public void setSendRoutes(boolean sendRoutes) {
    this.sendRoutes = sendRoutes;
  }

  public boolean isReceiveRoutes() {
    return receiveRoutes;
  }

  public void setReceiveRoutes(boolean receiveRoutes) {
    this.receiveRoutes = receiveRoutes;
  }

  public Map<String, Object> getCustomSettings() {
    return customSettings;
  }

  public void setCustomSettings(Map<String, Object> customSettings) {
    this.customSettings = customSettings;
  }

  /**
   * Overridden to use natural identifier.
   *
   * @see "https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/"
   */
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
    return Objects.hash(getNaturalId());
  }
}


