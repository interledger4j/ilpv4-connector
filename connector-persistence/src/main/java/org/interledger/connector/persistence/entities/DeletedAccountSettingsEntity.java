package org.interledger.connector.persistence.entities;

import static org.interledger.connector.persistence.entities.DataConstants.ColumnNames.ACCOUNT_RELATIONSHIP;
import static org.interledger.connector.persistence.entities.DataConstants.IndexNames.DELETED_ACCT_REL_IDX;
import static org.interledger.connector.persistence.entities.DataConstants.TableNames.DELETED_ACCOUNT_SETTINGS;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.persistence.AccountRelationshipConverter;
import org.interledger.connector.persistence.HashMapConverter;
import org.interledger.connector.persistence.LinkTypeConverter;
import org.interledger.link.LinkType;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

@Entity
@Access(AccessType.FIELD)
@Table(name = DELETED_ACCOUNT_SETTINGS, indexes = {
  @Index(name = DELETED_ACCT_REL_IDX, columnList = ACCOUNT_RELATIONSHIP)
})
@SuppressWarnings( {"PMD"})
public class DeletedAccountSettingsEntity extends AbstractEntity {

  @Id
  @GeneratedValue
  @Column(name = "ID")
  private Long id;

  @Column(name = "ACCOUNT_ID") // Hibernate treats this as unique, but Liquibase is explicit about uniqueness.
  private String accountId;

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
  private BigInteger maximumPacketAmount;

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
  DeletedAccountSettingsEntity() {
  }

  public DeletedAccountSettingsEntity(final AccountSettingsEntity accountSettings) {
    Objects.requireNonNull(accountSettings);

    this.accountId = accountSettings.getAccountId().value();
    this.description = accountSettings.getDescription();
    this.internal = accountSettings.isInternal();
    this.connectionInitiator = accountSettings.isConnectionInitiator();
    this.ilpAddressSegment = accountSettings.getIlpAddressSegment();
    this.accountRelationship = accountSettings.getAccountRelationship();
    this.linkType = accountSettings.getLinkType();
    this.assetCode = accountSettings.getAssetCode();
    this.assetScale = accountSettings.getAssetScale();
    this.maximumPacketAmount = accountSettings.getMaximumPacketAmount().orElse(null);
    this.balanceSettings = accountSettings.getBalanceSettings();
    this.rateLimitSettings = accountSettings.getRateLimitSettings();
    this.settlementEngineDetails = accountSettings.getSettlementEngineDetailsEntity();

    this.sendRoutes = accountSettings.sendRoutes();
    this.receiveRoutes = accountSettings.receiveRoutes();
    this.customSettings = accountSettings.getCustomSettings();
  }

  public AccountId getAccountId() {
    return AccountId.of(accountId);
  }

  public Long getId() {
    return this.id;
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

  public Optional<BigInteger> getMaximumPacketAmount() {
    return Optional.ofNullable(maximumPacketAmount);
  }

  public void setMaximumPacketAmount(Optional<BigInteger> maximumPacketAmount) {
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

  public boolean sendRoutes() {
    return sendRoutes;
  }

  public void setSendRoutes(boolean sendRoutes) {
    this.sendRoutes = sendRoutes;
  }

  public boolean receiveRoutes() {
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

    DeletedAccountSettingsEntity that = (DeletedAccountSettingsEntity) o;
    return Objects.equals(getAccountId(), that.getAccountId());
  }

  /**
   * Return the hashcode of the natural identifier.
   *
   * @return An integer representing the natural identifier for this entity.
   *
   * @see "https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/"
   */
  @Override
  public int hashCode() {
    return Objects.hash(getAccountId());
  }
}


