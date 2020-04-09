package org.interledger.connector.persistence.entities;

import static org.interledger.connector.persistence.entities.DataConstants.TableNames.STREAM_PAYMENTS;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.payments.StreamPaymentStatus;
import org.interledger.connector.payments.StreamPaymentType;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity for persisting a transaction. See {@link StreamPayment} for
 * javadoc of each field's meaning.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = STREAM_PAYMENTS)
@SuppressWarnings( {"PMD"})
public class StreamPaymentEntity {

  @Id
  @Column(name = "ID")
  private Long id;

  @Column(name = "STREAM_PAYMENT_ID")
  private String streamPaymentId;

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

  @Column(name = "SOURCE_ADDRESS")
  private String sourceAddress;

  @Column(name = "STATUS")
  @Enumerated(EnumType.STRING)
  private StreamPaymentStatus status;

  @Column(name = "TYPE")
  @Enumerated(EnumType.STRING)
  private StreamPaymentType type;

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

  public String getDestinationAddress() {
    return destinationAddress;
  }

  public void setDestinationAddress(String destinationAddress) {
    this.destinationAddress = destinationAddress;
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


  public String getStreamPaymentId() {
    return streamPaymentId;
  }

  public void setStreamPaymentId(String streamPaymentId) {
    this.streamPaymentId = streamPaymentId;
  }

  public String getSourceAddress() {
    return sourceAddress;
  }

  public void setSourceAddress(String sourceAddress) {
    this.sourceAddress = sourceAddress;
  }

  public StreamPaymentStatus getStatus() {
    return status;
  }

  public void setStatus(StreamPaymentStatus status) {
    this.status = status;
  }

  public StreamPaymentType getType() {
    return type;
  }

  public void setType(StreamPaymentType type) {
    this.type = type;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }

  public Instant getModifiedDate() {
    return modifiedDate;
  }

  public void setModifiedDate(Instant modifiedDate) {
    this.modifiedDate = modifiedDate;
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

    StreamPaymentEntity that = (StreamPaymentEntity) o;
    return Objects.equals(getStreamPaymentId(), that.getStreamPaymentId());
  }

  /**
   * Return the hashcode of the natural identifier.
   *
   * @return An integer representing the natural identifier for this entity.
   * @see "https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/"
   */
  @Override
  public int hashCode() {
    return Objects.hash(getStreamPaymentId());
  }

  @Override
  public String toString() {
    return "TransactionEntity{" +
      "id=" + id +
      ", streamPaymentId='" + streamPaymentId + '\'' +
      ", accountId=" + accountId +
      ", destinationAddress='" + destinationAddress + '\'' +
      ", assetCode='" + assetCode + '\'' +
      ", assetScale=" + assetScale +
      ", amount=" + amount +
      ", packetCount=" + packetCount +
      ", sourceAddress='" + sourceAddress + '\'' +
      ", status='" + status + '\'' +
      ", type='" + type + '\'' +
      ", createdDate=" + createdDate +
      ", modifiedDate=" + modifiedDate +
      '}';
  }
}


