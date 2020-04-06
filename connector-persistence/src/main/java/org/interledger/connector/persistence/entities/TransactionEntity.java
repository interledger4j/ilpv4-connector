package org.interledger.connector.persistence.entities;

import static org.interledger.connector.persistence.entities.DataConstants.TableNames.TRANSACTIONS;

import org.interledger.connector.accounts.AccountId;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Access(AccessType.FIELD)
@Table(name = TRANSACTIONS)
@SuppressWarnings( {"PMD"})
public class TransactionEntity {

  @Id
  @Column(name = "ID")
  private Long id;

  @Column(name = "REFERENCE_ID")
  private String referenceId;

  @Column(name = "ACCOUNT_ID")
  private AccountId accountId;

  @Column(name = "DESTINATION_ADDRESS")
  private String destinationAddress;

  @Column(name = "ASSET_CODE")
  private String assetCode;

  @Column(name = "ASSET_SCALE")
  private short assetScale;

  @Column(name = "AMOUNT")
  private BigInteger amount;

  @Column(name = "PACKET_COUNT")
  private int packetCount;

  @Column(name = "STATUS")
  private String status;

  @Column(name = "CREATED_DTTM", nullable = false, updatable = false)
  private Instant createdDate;

  @Column(name = "MODIFIED_DTTM", nullable = false)
  private Instant modifiedDate;

  public Long getId() {
    return id;
  }

  public AccountId getAccountId() {
    return accountId;
  }

  public void setAccountId(AccountId accountId) {
    this.accountId = accountId;
  }

  public String getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  public String getAssetCode() {
    return assetCode;
  }

  public void setAssetCode(String assetCode) {
    this.assetCode = assetCode;
  }

  public short getAssetScale() {
    return assetScale;
  }

  public void setAssetScale(short assetScale) {
    this.assetScale = assetScale;
  }

  public BigInteger getAmount() {
    return amount;
  }

  public void setAmount(BigInteger amount) {
    this.amount = amount;
  }

  public int getPacketCount() {
    return packetCount;
  }

  public void setPacketCount(int packetCount) {
    this.packetCount = packetCount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDestinationAddress() {
    return destinationAddress;
  }

  public void setDestinationAddress(String destinationAddress) {
    this.destinationAddress = destinationAddress;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public Instant getModifiedDate() {
    return modifiedDate;
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

    TransactionEntity that = (TransactionEntity) o;
    return Objects.equals(getReferenceId(), that.getReferenceId());
  }

  /**
   * Return the hashcode of the natural identifier.
   *
   * @return An integer representing the natural identifier for this entity.
   * @see "https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/"
   */
  @Override
  public int hashCode() {
    return Objects.hash(getReferenceId());
  }

  @Override
  public String toString() {
    return "TransactionEntity{" +
      "referenceId='" + referenceId + '\'' +
      ", accountId=" + accountId +
      ", destinationAddress='" + destinationAddress + '\'' +
      ", assetCode='" + assetCode + '\'' +
      ", assetScale=" + assetScale +
      ", amount=" + amount +
      ", packetCount=" + packetCount +
      ", status='" + status + '\'' +
      '}';
  }
}


